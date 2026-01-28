package com.matchimban.matchimban_api.vote.service;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.vote.dto.request.FinalSelectionRequest;
import com.matchimban.matchimban_api.vote.dto.response.FinalSelectionResponse;
import com.matchimban.matchimban_api.vote.dto.request.VoteSubmitRequest;
import com.matchimban.matchimban_api.vote.dto.response.CreateVoteResponse;
import com.matchimban.matchimban_api.vote.dto.response.VoteCandidatesResponse;
import com.matchimban.matchimban_api.vote.dto.response.VoteResultsResponse;
import com.matchimban.matchimban_api.vote.dto.response.VoteStatusResponse;
import com.matchimban.matchimban_api.vote.entity.*;
import com.matchimban.matchimban_api.vote.event.VoteCandidateGenerationRequestedEvent;
import com.matchimban.matchimban_api.vote.repository.MeetingFinalSelectionRepository;
import com.matchimban.matchimban_api.vote.repository.MeetingRestaurantCandidateRepository;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import com.matchimban.matchimban_api.vote.repository.VoteSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VoteSubmissionRepository voteSubmissionRepository;
    private final MeetingRestaurantCandidateRepository meetingRestaurantCandidateRepository;
    private final VoteCountService voteCountService;
    private final MeetingFinalSelectionRepository meetingFinalSelectionRepository;


    @Transactional
    public CreateVoteResponse createVote(Long meetingId, Long memberId) {
        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found"));

        if (!meeting.getHostMemberId().equals(memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_host");
        }

        boolean isActiveParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActiveParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_active_participant");
        }

        long activeCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );
        if (activeCount != meeting.getTargetHeadcount()) {
            throw new ApiException(HttpStatus.CONFLICT, "vote_create_not_ready_headcount",
                    "activeCount=" + activeCount + ", target=" + meeting.getTargetHeadcount());
        }

        return voteRepository.findByMeetingIdAndRound(meetingId, (short) 1)
                .filter(v -> v.getStatus() != VoteStatus.FAILED)
                .map(v -> new CreateVoteResponse(v.getId()))
                .orElseGet(() -> createNewVotesAndPublishEvent(meeting));
    }

    private CreateVoteResponse createNewVotesAndPublishEvent(Meeting meeting) {
        Vote v1 = Vote.builder()
                .meeting(meeting)
                .round((short) 1)
                .status(VoteStatus.GENERATING)
                .generatedAt(null)
                .countedAt(null)
                .build();

        Vote v2 = Vote.builder()
                .meeting(meeting)
                .round((short) 2)
                .status(VoteStatus.GENERATING)
                .generatedAt(null)
                .countedAt(null)
                .build();

        Vote saved1 = voteRepository.save(v1);
        Vote saved2 = voteRepository.save(v2);

        eventPublisher.publishEvent(new VoteCandidateGenerationRequestedEvent(
                meeting.getId(), saved1.getId(), saved2.getId()
        ));

        return new CreateVoteResponse(saved1.getId());
    }

    @Transactional(readOnly = true)
    public VoteCandidatesResponse getCandidates(Long meetingId, Long voteId, Long memberId) {

        boolean isActive = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActive) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_active_participant");
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "vote_not_found"));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "vote_not_found");
        }

        if (vote.getStatus() != VoteStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "vote_not_open");
        }

        List<VoteCandidatesResponse.Candidate> items = meetingRestaurantCandidateRepository.findCandidateDtosByVoteId(voteId);
        if (items.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "vote_candidates_not_ready");
        }

        return new VoteCandidatesResponse(items);
    }

    @Transactional
    public void submitVote(Long meetingId, Long voteId, Long memberId, VoteSubmitRequest request) {

        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndMemberId(meetingId, memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_participant"));

        if (participant.getStatus() != MeetingParticipant.Status.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_active_participant");
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "vote_not_found"));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "vote_not_found");
        }

        if (vote.getStatus() != VoteStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "vote_not_open");
        }

        if (voteSubmissionRepository.existsByVoteIdAndParticipantId(voteId, participant.getId())) {
            return;
        }

        int expectedCount = vote.getMeeting().getSwipeCount();
        int actualCount = (request.getItems() == null) ? 0 : request.getItems().size();
        if (actualCount != expectedCount) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_request",
                    "items.size=" + actualCount + ", expected=" + expectedCount
            );
        }

        List<Long> candidateIds = request.getItems().stream()
                .map(VoteSubmitRequest.Item::getCandidateId)
                .toList();

        long distinctCount = candidateIds.stream().distinct().count();
        if (distinctCount != candidateIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", "duplicate_candidate_id");
        }

        List<Long> validIds = meetingRestaurantCandidateRepository.findIdsByVoteIdAndIdIn(voteId, candidateIds);
        if (validIds.size() != candidateIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", "candidate_not_in_vote");
        }

        Map<Long, MeetingRestaurantCandidate> candidateMap =
                meetingRestaurantCandidateRepository.findAllById(candidateIds).stream()
                        .collect(Collectors.toMap(MeetingRestaurantCandidate::getId, c -> c));

        List<VoteSubmission> submissions = request.getItems().stream()
                .map(item -> VoteSubmission.builder()
                        .vote(vote)
                        .participant(participant)
                        .candidateRestaurant(candidateMap.get(item.getCandidateId()))
                        .choice(item.getChoice())
                        .build())
                .toList();

        voteSubmissionRepository.saveAll(submissions);

        long totalCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );
        long submittedCount = voteSubmissionRepository.countDistinctParticipantsByVoteId(voteId);

        if (submittedCount >= totalCount) {
            boolean started = voteCountService.tryStartCounting(voteId);
            if (started) {
                voteCountService.countAsync(voteId);
            }
        }
    }

    @Transactional(readOnly = true)
    public VoteStatusResponse getVoteStatus(Long meetingId, Long voteId, Long memberId) {

        boolean isActive = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActive) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_active_participant");
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "vote_not_found"));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "vote_not_found");
        }

        long totalCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );

        long submittedCount = voteSubmissionRepository.countDistinctParticipantsByVoteId(voteId);

        return new VoteStatusResponse(vote.getStatus(), submittedCount, totalCount);
    }

    @Transactional(readOnly = true)
    public VoteResultsResponse getResults(Long meetingId, Long voteId, Long memberId) {

        boolean isActive = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActive) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_active_participant");
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "vote_not_found"));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "vote_not_found");
        }

        if (vote.getStatus() != VoteStatus.COUNTED) {
            throw new ApiException(HttpStatus.CONFLICT, "vote_not_counted_yet");
        }

        List<VoteResultsResponse.Item> items = meetingRestaurantCandidateRepository.findTop3ResultItems(voteId);

        if (items.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error", "top3_missing");
        }

        return new VoteResultsResponse(items);
    }

    @Transactional
    public void finalizeSelection(Long meetingId, Long voteId, Long memberId, FinalSelectionRequest request) {

        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found"));

        if (!Objects.equals(meeting.getHostMemberId(), memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_host");
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "vote_not_found"));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "vote_not_found");
        }

        if (vote.getStatus() != VoteStatus.COUNTED) {
            throw new ApiException(HttpStatus.CONFLICT, "vote_not_counted_yet");
        }

        if (meetingFinalSelectionRepository.existsByMeetingId(meetingId)) {
            return;
        }

        MeetingRestaurantCandidate candidate = meetingRestaurantCandidateRepository.findByIdWithVoteAndMeeting(request.getCandidateId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "candidate_not_found"));

        if (candidate.getVote() == null || candidate.getVote().getId() == null || !candidate.getVote().getId().equals(voteId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", "candidate_not_in_vote");
        }

        Integer rr = candidate.getResultRank();
        if (rr == null || rr < 1 || rr > 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", "candidate_not_in_top3");
        }

        MeetingFinalSelection fs = MeetingFinalSelection.builder()
                .meeting(meeting)
                .finalCandidate(candidate)
                .build();

        meetingFinalSelectionRepository.save(fs);
    }

    @Transactional(readOnly = true)
    public FinalSelectionResponse getFinalSelection(Long meetingId, Long memberId) {

        boolean isActive = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActive) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_active_participant");
        }

        return meetingFinalSelectionRepository.findFinalSelectionResponseByMeetingId(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "final_selection_not_found"));
    }


}
