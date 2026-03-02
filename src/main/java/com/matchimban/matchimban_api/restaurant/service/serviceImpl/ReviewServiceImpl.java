package com.matchimban.matchimban_api.restaurant.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import com.matchimban.matchimban_api.restaurant.dto.request.ReviewCreateRequest;
import com.matchimban.matchimban_api.restaurant.dto.request.ReviewUpdateRequest;
import com.matchimban.matchimban_api.restaurant.dto.response.CreateReviewResponse;
import com.matchimban.matchimban_api.restaurant.dto.response.ReviewDetailResponse;
import com.matchimban.matchimban_api.restaurant.entity.Review;
import com.matchimban.matchimban_api.restaurant.error.ReviewErrorCode;
import com.matchimban.matchimban_api.restaurant.repository.ReviewRepository;
import com.matchimban.matchimban_api.vote.repository.MeetingFinalSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.matchimban.matchimban_api.global.time.TimeKst.toKstLocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements com.matchimban.matchimban_api.restaurant.service.ReviewService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingFinalSelectionRepository meetingFinalSelectionRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public CreateReviewResponse createReview(Long meetingId, Long memberId, ReviewCreateRequest request) {

        var meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        boolean isActive = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActive) {
            throw new ApiException(ReviewErrorCode.FORBIDDEN_NOT_ACTIVE_PARTICIPANT);
        }

        var finalSelection = meetingFinalSelectionRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ApiException(ReviewErrorCode.FINAL_SELECTION_NOT_FOUND));

        var restaurant = finalSelection.getFinalCandidate().getRestaurant();

        if (reviewRepository.existsActiveReview(meetingId, memberId)) {
            throw new ApiException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        try {
            var memberRef = memberRepository.getReferenceById(memberId);

            Review review = Review.builder()
                    .meeting(meeting)
                    .member(memberRef)
                    .restaurant(restaurant)
                    .rating(request.getRating())
                    .content(request.getContent())
                    .isDeleted(false)
                    .build();

            Review saved = reviewRepository.save(review);
            return new CreateReviewResponse(saved.getId());

        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public ReviewDetailResponse updateReview(Long reviewId, Long memberId, ReviewUpdateRequest request) {

        Review review = reviewRepository.findActiveByIdAndMemberId(reviewId, memberId)
                .orElseThrow(() -> new ApiException(ReviewErrorCode.REVIEW_NOT_FOUND));

        review.update(request.getRating(), request.getContent());

        var row = reviewRepository.findMyReviewDetailRow(reviewId, memberId)
                .orElseThrow(() -> new ApiException(ReviewErrorCode.REVIEW_NOT_FOUND));

        return new ReviewDetailResponse(
                row.getReviewId(),
                row.getMeetingId(),
                row.getRating(),
                row.getContent(),
                toKstLocalDateTime(row.getCreatedAt()),
                toKstLocalDateTime(row.getUpdatedAt()),
                row.getRestaurantId(),
                row.getRestaurantName(),
                row.getRestaurantImageUrl1(),
                row.getCategoryName(),
                row.getCategoryEmoji()
        );
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findActiveByIdAndMemberId(reviewId, memberId)
                .orElseThrow(() -> new ApiException(ReviewErrorCode.REVIEW_NOT_FOUND));

        review.delete();
    }
}