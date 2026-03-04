package com.matchimban.matchimban_api.meeting.service.serviceImpl;

import com.matchimban.matchimban_api.auth.jwt.GuestJwtTokenProvider;
import com.matchimban.matchimban_api.auth.jwt.GuestPrincipal;
import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.dto.request.QuickMeetingEnterRequest;
import com.matchimban.matchimban_api.meeting.dto.response.QuickMeetingEnterResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.meeting.service.QuickMeetingService;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuickMeetingServiceImpl implements QuickMeetingService {

    private static final long GUEST_TOKEN_GRACE_SECONDS = 300;

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MemberRepository memberRepository;
    private final EntityManager entityManager;
    private final GuestJwtTokenProvider guestJwtTokenProvider;

    @Override
    @Transactional
    public EnterResult enter(Object principal, QuickMeetingEnterRequest request) {

        Meeting meeting = meetingRepository
                .findByInviteCodeAndIsDeletedFalseForUpdate(request.getInviteCode())
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        if (!meeting.isQuickMeeting()) {
            throw new ApiException(MeetingErrorCode.NOT_QUICK_MEETING);
        }

        Actor actor = resolveActor(principal, request);

        upsertParticipant(meeting, actor.memberId());

        ResponseCookie cookie = null;
        String guestUuid = null;

        if (actor.isGuest()) {
            Member guestMember = actor.member();
            guestUuid = guestMember.getGuestUuid() != null ? guestMember.getGuestUuid().toString() : null;

            Instant requestedExpiresAt = meeting.getVoteDeadlineAt().plusSeconds(GUEST_TOKEN_GRACE_SECONDS);
            GuestJwtTokenProvider.GuestTokenIssueResult issued =
                    guestJwtTokenProvider.issueGuestAccessToken(guestMember.getId(), meeting.getId(), requestedExpiresAt);

            cookie = guestJwtTokenProvider.createGuestAccessTokenCookie(issued.token(), issued.expiresAt());
        }

        return new EnterResult(
                new QuickMeetingEnterResponse(meeting.getId(), guestUuid, meeting.getVoteDeadlineAt()),
                cookie
        );
    }

    private record Actor(Long memberId, boolean isGuest, Member member) {}

    private Actor resolveActor(Object principal, QuickMeetingEnterRequest request) {
        if (principal instanceof MemberPrincipal mp) {
            Member memberRef = entityManager.getReference(Member.class, mp.memberId());
            return new Actor(mp.memberId(), false, memberRef);
        }

        if (principal instanceof GuestPrincipal gp) {
            Member guest = memberRepository.findById(gp.memberId())
                    .orElseThrow(() -> new ApiException(MeetingErrorCode.PARTICIPANT_NOT_FOUND));
            return new Actor(guest.getId(), true, guest);
        }

        UUID guestUuid = parseOrCreateGuestUuid(request.getGuestUuid());
        Member guest = memberRepository.findByGuestUuid(guestUuid)
                .orElseGet(() -> memberRepository.save(buildGuestMember(guestUuid)));

        return new Actor(guest.getId(), true, guest);
    }

    private UUID parseOrCreateGuestUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(MeetingErrorCode.INVALID_GUEST_UUID);
        }
    }

    private Member buildGuestMember(UUID guestUuid) {
        Instant now = Instant.now();
        String suffix = guestUuid.toString().replace("-", "");
        suffix = suffix.length() >= 4 ? suffix.substring(0, 4) : suffix;

        return Member.builder()
                .nickname("게스트-" + suffix)
                .profileImageUrl(null)
                .thumbnailImageUrl(null)
                .status(MemberStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .isGuest(true)
                .guestUuid(guestUuid)
                .build();
    }

    private long upsertParticipant(Meeting meeting, Long memberId) {
        Long meetingId = meeting.getId();

        MeetingParticipant existing = meetingParticipantRepository
                .findByMeetingIdAndMemberId(meetingId, memberId)
                .orElse(null);

        if (existing != null && existing.getStatus() == MeetingParticipant.Status.ACTIVE) {
            return meetingParticipantRepository.countByMeetingIdAndStatus(
                    meetingId, MeetingParticipant.Status.ACTIVE
            );
        }

        if (existing != null) {
            existing.reactivate();
            return meetingParticipantRepository.countByMeetingIdAndStatus(
                    meetingId, MeetingParticipant.Status.ACTIVE
            );
        }

        long activeCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );

        if (activeCount >= meeting.getTargetHeadcount()) {
            throw new ApiException(MeetingErrorCode.MEETING_FULL);
        }

        Member memberRef = entityManager.getReference(Member.class, memberId);

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .member(memberRef)
                .role(MeetingParticipant.Role.MEMBER)
                .status(MeetingParticipant.Status.ACTIVE)
                .build();

        meetingParticipantRepository.save(participant);

        return activeCount + 1;
    }
}