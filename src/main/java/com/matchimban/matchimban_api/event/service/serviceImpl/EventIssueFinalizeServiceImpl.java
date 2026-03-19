package com.matchimban.matchimban_api.event.service.serviceImpl;

import com.matchimban.matchimban_api.event.entity.Event;
import com.matchimban.matchimban_api.event.entity.EventCoupon;
import com.matchimban.matchimban_api.event.entity.EventCouponStatus;
import com.matchimban.matchimban_api.event.entity.EventIssueFailureReason;
import com.matchimban.matchimban_api.event.entity.EventParticipant;
import com.matchimban.matchimban_api.event.repository.EventCouponRepository;
import com.matchimban.matchimban_api.event.repository.EventParticipantRepository;
import com.matchimban.matchimban_api.event.repository.EventRepository;
import com.matchimban.matchimban_api.event.service.EventIssueFinalizeService;
import com.matchimban.matchimban_api.member.entity.Member;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventIssueFinalizeServiceImpl implements EventIssueFinalizeService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventCouponRepository eventCouponRepository;

    @Override
    @Transactional
    public FinalizeResult finalizeIssue(Long eventId, Long memberId) {
        Event event = eventRepository.findByIdForIssue(eventId)
                .orElse(null);
        Instant now = Instant.now();
        if (event == null) {
            return new FinalizeResult(false, EventIssueFailureReason.EVENT_ENDED, null, null, null);
        }
        if (now.isBefore(event.getStartAt())) {
            return new FinalizeResult(false, EventIssueFailureReason.EVENT_NOT_STARTED, null, null, null);
        }
        if (!now.isBefore(event.getEndAt())) {
            return new FinalizeResult(false, EventIssueFailureReason.EVENT_ENDED, null, null, null);
        }
        if (eventParticipantRepository.existsByEventIdAndMemberId(eventId, memberId)) {
            return new FinalizeResult(false, EventIssueFailureReason.ALREADY_ISSUED, null, null, null);
        }
        if (event.getIssuedCount() >= event.getCapacity()) {
            return new FinalizeResult(false, EventIssueFailureReason.SOLD_OUT, null, null, null);
        }

        Instant issuedAt = now;
        Instant expiredAt = event.getEndAt();

        try {
            EventParticipant participant = eventParticipantRepository.save(
                    EventParticipant.builder()
                            .event(event)
                            .member(Member.builder().id(memberId).build())
                            .build()
            );

            EventCoupon coupon = eventCouponRepository.save(
                    EventCoupon.builder()
                            .eventParticipant(participant)
                            .member(Member.builder().id(memberId).build())
                            .couponType(event.getCouponType())
                            .status(EventCouponStatus.ISSUED)
                            .expiredAt(expiredAt)
                            .build()
            );

            event.increaseIssuedCount(1);

            return new FinalizeResult(true, null, coupon.getId(), issuedAt, expiredAt);
        } catch (DataIntegrityViolationException ex) {
            return new FinalizeResult(false, EventIssueFailureReason.ALREADY_ISSUED, null, null, null);
        }
    }
}
