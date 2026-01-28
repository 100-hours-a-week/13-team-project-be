package com.matchimban.matchimban_api.vote.service;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.member.entity.MemberCategoryMapping;
import com.matchimban.matchimban_api.member.entity.enums.MemberCategoryRelationType;
import com.matchimban.matchimban_api.member.repository.FoodCategoryRepository;
import com.matchimban.matchimban_api.member.repository.MemberCategoryMappingRepository;
import com.matchimban.matchimban_api.restaurant.entity.Restaurant;
import com.matchimban.matchimban_api.restaurant.repository.RestaurantRepository;
import com.matchimban.matchimban_api.restaurant.repository.ReviewRepository;
import com.matchimban.matchimban_api.vote.ai.RecommendationClient;
import com.matchimban.matchimban_api.vote.ai.dto.AiRecommendationRequest;
import com.matchimban.matchimban_api.vote.ai.dto.AiRecommendationResponse;
import com.matchimban.matchimban_api.vote.entity.MeetingRestaurantCandidate;
import com.matchimban.matchimban_api.vote.entity.Vote;
import com.matchimban.matchimban_api.vote.repository.MeetingRestaurantCandidateRepository;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.matchimban.matchimban_api.member.entity.enums.FoodCategoryType.CATEGORY;

@Service
@RequiredArgsConstructor
public class VoteCandidateAsyncService {

    private static final Logger LOG = LoggerFactory.getLogger(VoteCandidateAsyncService.class);

    private final VoteRepository voteRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final MemberCategoryMappingRepository memberCategoryMappingRepository;
    private final RecommendationClient recommendationClient;

    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final MeetingRestaurantCandidateRepository candidateRepository;

    @Transactional
    public void generateCandidates(Long meetingId, Long round1VoteId, Long round2VoteId) {
        try {
            Vote v1 = voteRepository.findById(round1VoteId).orElseThrow();
            Vote v2 = voteRepository.findById(round2VoteId).orElseThrow();

            Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found"));

            List<Long> memberIds = meetingParticipantRepository.findActiveMemberIds(meetingId);
            if (memberIds.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "vote_create_not_ready_headcount", "no_active_participants");
            }

            var categories = foodCategoryRepository.findByCategoryType(CATEGORY);
            Map<String, Integer> like = new LinkedHashMap<>();
            Map<String, Integer> dislike = new LinkedHashMap<>();
            for (var c : categories) {
                like.put(c.getCategoryName(), 0);
                dislike.put(c.getCategoryName(), 0);
            }

            List<MemberCategoryMapping> mappings = memberCategoryMappingRepository.findByMemberIdsWithCategory(memberIds);
            for (MemberCategoryMapping m : mappings) {
                String key = m.getCategory().getCategoryName();
                if (!like.containsKey(key)) continue;

                if (m.getRelationType() == MemberCategoryRelationType.PREFERENCE) {
                    like.put(key, like.get(key) + 1);
                } else if (m.getRelationType() == MemberCategoryRelationType.DISLIKE) {
                    dislike.put(key, dislike.get(key) + 1);
                }
            }

            String requestId = "vote_" + meetingId + "_" + round1VoteId;

            ZoneId zone = ZoneId.of("Asia/Seoul");
            OffsetDateTime startTime = meeting.getScheduledAt().atZone(zone).toOffsetDateTime();

            AiRecommendationRequest req = AiRecommendationRequest.builder()
                    .memberId(meeting.getHostMemberId())
                    .requestId(requestId)
                    .meeting(AiRecommendationRequest.Meeting.builder()
                            .startTime(startTime.toString())
                            .headcount(meeting.getTargetHeadcount())
                            .build())
                    .location(AiRecommendationRequest.Location.builder()
                            .lat(meeting.getLocationLat().doubleValue())
                            .lng(meeting.getLocationLng().doubleValue())
                            .radiusM(meeting.getSearchRadiusM())
                            .build())
                    .swipe(AiRecommendationRequest.Swipe.builder()
                            .cardLimit(meeting.getSwipeCount())
                            .build())
                    .preferences(AiRecommendationRequest.Preferences.builder()
                            .like(like)
                            .dislike(dislike)
                            .build())
                    .build();

            AiRecommendationResponse res = recommendationClient.recommend(req);
            if (res == null || res.getRestaurants() == null || res.getRestaurants().isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "no_restaurants_found");
            }

            int expected = meeting.getSwipeCount() * 2;
            int actual = res.getRestaurants().size();
            if (actual != expected) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "recommendation_failed",
                        "expected=" + expected + ", actual=" + actual
                );
            }

            List<AiRecommendationResponse.Restaurant> list = res.getRestaurants();
            int half = expected / 2;

            List<AiRecommendationResponse.Restaurant> r1 = list.subList(0, half);
            List<AiRecommendationResponse.Restaurant> r2 = list.subList(half, expected);

            candidateRepository.deleteByVoteId(v1.getId());
            candidateRepository.deleteByVoteId(v2.getId());

            int savedR1 = saveCandidates(v1, r1);
            int savedR2 = saveCandidates(v2, r2);

            if (savedR1 != half) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "no_restaurants_found",
                        "savedR1=" + savedR1 + ", expectedHalf=" + half
                );
            }
            if (savedR2 != half) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "no_restaurants_found",
                        "savedR2=" + savedR2 + ", expectedHalf=" + half
                );
            }

            LocalDateTime now = LocalDateTime.now();
            v1.markOpen(now);
            v2.markReserved(now);

            LOG.info("Vote candidates generated. meetingId={}, v1Saved={}, v2Saved={}", meetingId, savedR1, savedR2);

        } catch (Exception e) {
            LOG.error("Vote candidate generation failed. meetingId={}, vote1={}, vote2={}",
                    meetingId, round1VoteId, round2VoteId, e);
            markVotesFailedInNewTx(round1VoteId, round2VoteId);
        }
    }

    private int saveCandidates(Vote vote, List<AiRecommendationResponse.Restaurant> items) {
        if (items == null || items.isEmpty()) return 0;

        List<Long> ids = items.stream()
                .map(AiRecommendationResponse.Restaurant::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) return 0;

        List<Restaurant> restaurants = restaurantRepository.findByIdIn(ids);
        Map<Long, Restaurant> byId = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, Function.identity()));

        Map<Long, Double> avgRatingByRestaurantId =
                reviewRepository.findAvgRatingsByRestaurantIds(ids).stream()
                        .collect(Collectors.toMap(
                                ReviewRepository.RestaurantAvgRatingRow::getRestaurantId,
                                ReviewRepository.RestaurantAvgRatingRow::getAvgRating
                        ));

        List<MeetingRestaurantCandidate> candidates = new ArrayList<>(items.size());

        for (AiRecommendationResponse.Restaurant r : items) {
            Restaurant restaurant = byId.get(r.getStoreId());
            if (restaurant == null) {
                LOG.warn("Restaurant not found for storeId={}. skip", r.getStoreId());
                continue;
            }

            Double avg = avgRatingByRestaurantId.get(r.getStoreId());
            BigDecimal reviewAvg = (avg == null)
                    ? BigDecimal.valueOf(0.0).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

            candidates.add(MeetingRestaurantCandidate.builder()
                    .vote(vote)
                    .restaurant(restaurant)
                    .distanceM(r.getDistanceM())
                    .baseRank(r.getRank())
                    .rating(reviewAvg)
                    .aiScore(r.getFinalScore())
                    .resultRank(null)
                    .likeCount(0)
                    .dislikeCount(0)
                    .neutralCount(0)
                    .build());
        }

        if (candidates.isEmpty()) return 0;

        candidateRepository.saveAll(candidates);
        return candidates.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markVotesFailedInNewTx(Long round1VoteId, Long round2VoteId) {
        voteRepository.findById(round1VoteId).ifPresent(Vote::markFailed);
        voteRepository.findById(round2VoteId).ifPresent(Vote::markFailed);
    }
}
