package com.matchimban.matchimban_api.vote.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.global.error.code.CommonErrorCode;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
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
import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import com.matchimban.matchimban_api.vote.error.VoteErrorCode;
import com.matchimban.matchimban_api.vote.event.VoteCandidateGenerationRequestedEvent;
import com.matchimban.matchimban_api.vote.repository.MeetingFinalSelectionRepository;
import com.matchimban.matchimban_api.vote.repository.MeetingRestaurantCandidateRepository;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import com.matchimban.matchimban_api.vote.repository.VoteSubmissionRepository;
import com.matchimban.matchimban_api.vote.service.VoteCountService;
import com.matchimban.matchimban_api.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

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
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getHostMemberId().equals(memberId)) {
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_HOST);
        }

        boolean isActiveParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActiveParticipant) {
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_ACTIVE_PARTICIPANT);
        }

        long activeCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );
        if (activeCount != meeting.getTargetHeadcount()) {
            throw new ApiException(VoteErrorCode.VOTE_CREATE_NOT_READY_HEADCOUNT,
                    "activeCount=" + activeCount + ", target=" + meeting.getTargetHeadcount());
        }

        Vote v1 = voteRepository.findByMeetingIdAndRound(meetingId, (short) 1)
                .orElse(null);

        if (v1 == null) {
            return createNewVotesAndPublishEvent(meeting);
        }

        Vote v2 = voteRepository.findByMeetingIdAndRound(meetingId, (short) 2)
                .orElseThrow(() -> new ApiException(CommonErrorCode.INTERNAL_SERVER_ERROR, "round2_missing"));

        VoteStatus s1 = v1.getStatus();

        if (s1 == VoteStatus.OPEN || s1 == VoteStatus.COUNTING || s1 == VoteStatus.COUNTED) {
            return new CreateVoteResponse(v1.getId());
        }

        if (s1 == VoteStatus.GENERATING) {
            return new CreateVoteResponse(v1.getId());
        }

        if (s1 == VoteStatus.FAILED) {
            boolean acquired = voteRepository.updateStatusIfMatch(v1.getId(), VoteStatus.FAILED, VoteStatus.GENERATING) == 1;
            voteRepository.updateStatusIfMatch(v2.getId(), VoteStatus.FAILED, VoteStatus.GENERATING);
            voteRepository.updateStatusIfMatch(v2.getId(), VoteStatus.RESERVED, VoteStatus.GENERATING);

            if (acquired) {
                meetingRestaurantCandidateRepository.deleteByVoteId(v1.getId());
                meetingRestaurantCandidateRepository.deleteByVoteId(v2.getId());

                eventPublisher.publishEvent(new VoteCandidateGenerationRequestedEvent(
                        meeting.getId(), v1.getId(), v2.getId()
                ));
            }

            return new CreateVoteResponse(v1.getId());
        }

        return new CreateVoteResponse(v1.getId());
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
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_ACTIVE_PARTICIPANT);
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.VOTE_NOT_FOUND));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_FOUND);
        }

        if (vote.getStatus() != VoteStatus.OPEN) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_OPEN);
        }

        List<VoteCandidatesResponse.Candidate> items = meetingRestaurantCandidateRepository.findCandidateDtosByVoteId(voteId);
        if (items.isEmpty()) {
            throw new ApiException(VoteErrorCode.VOTE_CANDIDATES_NOT_READY);
        }

        return new VoteCandidatesResponse(items);
    }

    @Transactional
    public void submitVote(Long meetingId, Long voteId, Long memberId, VoteSubmitRequest request) {

        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndMemberId(meetingId, memberId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.FORBIDDEN_NOT_PARTICIPANT));

        if (participant.getStatus() != MeetingParticipant.Status.ACTIVE) {
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_ACTIVE_PARTICIPANT);
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.VOTE_NOT_FOUND));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_FOUND);
        }

        if (vote.getStatus() != VoteStatus.OPEN) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_OPEN);
        }

        if (voteSubmissionRepository.existsByVoteIdAndParticipantId(voteId, participant.getId())) {
            return;
        }

        int expectedCount = vote.getMeeting().getSwipeCount();
        int actualCount = (request.getItems() == null) ? 0 : request.getItems().size();
        if (actualCount != expectedCount) {
            throw new ApiException(CommonErrorCode.VALIDATION_FAILED, "items.size=" + actualCount + ", expected=" + expectedCount
            );
        }

        List<Long> candidateIds = request.getItems().stream()
                .map(VoteSubmitRequest.Item::getCandidateId)
                .toList();

        long distinctCount = candidateIds.stream().distinct().count();
        if (distinctCount != candidateIds.size()) {
            throw new ApiException(CommonErrorCode.VALIDATION_FAILED, "duplicate_candidate_id");
        }

        List<Long> validIds = meetingRestaurantCandidateRepository.findIdsByVoteIdAndIdIn(voteId, candidateIds);
        if (validIds.size() != candidateIds.size()) {
            throw new ApiException(CommonErrorCode.VALIDATION_FAILED, "candidate_not_in_vote");
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
                voteCountService.countSync(voteId);
            }
        }
    }

    @Transactional(readOnly = true)
    public VoteStatusResponse getVoteStatus(Long meetingId, Long voteId, Long memberId) {

        boolean isActive = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActive) {
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_ACTIVE_PARTICIPANT);
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.VOTE_NOT_FOUND));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_FOUND);
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
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_ACTIVE_PARTICIPANT);
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.VOTE_NOT_FOUND));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_FOUND);
        }

        if (vote.getStatus() != VoteStatus.COUNTED) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_COUNTED_YET);
        }

        List<VoteResultsResponse.Item> items =
                meetingRestaurantCandidateRepository.findTop3ResultItems(voteId);

        if (items.isEmpty()) {
            throw new ApiException(CommonErrorCode.INTERNAL_SERVER_ERROR, "top3_missing");
        }

        Long hostMemberId = vote.getMeeting().getHostMemberId();
        return new VoteResultsResponse(items, hostMemberId);
    }

    @Transactional
    public void startRevote(Long meetingId, Long voteId, Long memberId) {

        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        if (!Objects.equals(meeting.getHostMemberId(), memberId)) {
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_HOST);
        }

        if (meetingFinalSelectionRepository.existsByMeetingId(meetingId)) {
            throw new ApiException(VoteErrorCode.FINAL_ALREADY_SELECTED);
        }

        Vote v1 = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.VOTE_NOT_FOUND));

        if (v1.getMeeting() == null || !Objects.equals(v1.getMeeting().getId(), meetingId)) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_FOUND);
        }
        if (v1.getRound() != 1) {
            throw new ApiException(CommonErrorCode.VALIDATION_FAILED, "not_round1_vote");
        }
        if (v1.getStatus() != VoteStatus.COUNTED) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_COUNTED_YET);
        }

        Instant deadline = meeting.getVoteDeadlineAt();
        Instant now = Instant.now();
        if (!now.isBefore(deadline)) {
            throw new ApiException(VoteErrorCode.VOTE_DEADLINE_PASSED,
                    "now=" + now + ", deadline=" + deadline);
        }

        Vote v2 = voteRepository.findByMeetingIdAndRound(meetingId, (short) 2)
                .orElseThrow(() -> new ApiException(CommonErrorCode.INTERNAL_SERVER_ERROR, "round2_missing"));

        if (!meetingRestaurantCandidateRepository.existsByVoteId(v2.getId())) {
            throw new ApiException(VoteErrorCode.VOTE_CANDIDATES_NOT_READY);
        }

        if (v2.getStatus() == VoteStatus.OPEN) {
            return;
        }

        if (v2.getStatus() == VoteStatus.RESERVED) {
            v2.markOpen(now);
            return;
        }

        throw new ApiException(VoteErrorCode.REVOTE_NOT_AVAILABLE, "status=" + v2.getStatus());
    }

    @Transactional
    public void finalizeSelection(Long meetingId, Long voteId, Long memberId, FinalSelectionRequest request) {

        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        if (!Objects.equals(meeting.getHostMemberId(), memberId)) {
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_HOST);
        }

        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.VOTE_NOT_FOUND));

        if (vote.getMeeting() == null || !vote.getMeeting().getId().equals(meetingId)) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_FOUND);
        }

        if (vote.getStatus() != VoteStatus.COUNTED) {
            throw new ApiException(VoteErrorCode.VOTE_NOT_COUNTED_YET);
        }

        if (meetingFinalSelectionRepository.existsByMeetingId(meetingId)) {
            return;
        }

        MeetingRestaurantCandidate candidate = meetingRestaurantCandidateRepository.findByIdWithVoteAndMeeting(request.getCandidateId())
                .orElseThrow(() -> new ApiException(VoteErrorCode.CANDIDATE_NOT_FOUND));

        if (candidate.getVote() == null || candidate.getVote().getId() == null || !candidate.getVote().getId().equals(voteId)) {
            throw new ApiException(CommonErrorCode.VALIDATION_FAILED, "candidate_not_in_vote");
        }

        Integer rr = candidate.getFinalRank();
        if (rr == null || rr < 1 || rr > 3) {
            throw new ApiException(CommonErrorCode.VALIDATION_FAILED, "candidate_not_in_top3");
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
            throw new ApiException(VoteErrorCode.FORBIDDEN_NOT_ACTIVE_PARTICIPANT);
        }

        return meetingFinalSelectionRepository.findFinalSelectionResponseByMeetingId(meetingId)
                .orElseThrow(() -> new ApiException(VoteErrorCode.FINAL_SELECTION_NOT_FOUND));
    }


}
