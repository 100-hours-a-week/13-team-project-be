package com.matchimban.matchimban_api.notification.service;

import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.notification.config.NotificationProperties;
import com.matchimban.matchimban_api.notification.entity.NotificationJobType;
import com.matchimban.matchimban_api.notification.entity.NotificationScheduleJob;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.repository.NotificationRepository;
import com.matchimban.matchimban_api.notification.repository.NotificationScheduleJobRepository;
import com.matchimban.matchimban_api.notification.repository.NotificationTokenRepository;
import com.matchimban.matchimban_api.restaurant.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduleService {

    private final NotificationProperties notificationProperties;
    private final NotificationScheduleJobRepository notificationScheduleJobRepository;
    private final NotificationCommandService notificationCommandService;
    private final NotificationRepository notificationRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final NotificationTokenRepository notificationTokenRepository;
    private final ReviewRepository reviewRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final EntityManager entityManager;

    private final String workerId = "noti-schedule-" + UUID.randomUUID();

    @Transactional
    public void scheduleReviewRequestJobs(Long meetingId, Long voteId, List<Long> memberIds, Instant runAt) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueMemberIds = new LinkedHashSet<>(memberIds);
        for (Long memberId : uniqueMemberIds) {
            if (memberId == null) {
                continue;
            }

            Member memberRef = entityManager.getReference(Member.class, memberId);
            String jobKey = "REVIEW_REQUEST:" + meetingId + ":" + voteId + ":" + memberId;

            NotificationScheduleJob job = NotificationScheduleJob.builder()
                    .jobType(NotificationJobType.REVIEW_REQUEST)
                    .member(memberRef)
                    .targetType("MEETING")
                    .targetId(meetingId)
                    .runAt(runAt)
                    .jobKey(jobKey)
                    .nextAttemptAt(runAt)
                    .build();

            try {
                notificationScheduleJobRepository.save(job);
            } catch (DataIntegrityViolationException ignored) {
                // UNIQUE(member_id, job_key)에 걸리면 이미 스케줄된 작업으로 간주한다.
            }
        }
    }

    @Transactional
    public void tick() {
        if (!notificationProperties.getSchedule().isEnabled()) {
            return;
        }

        Instant staleBefore = Instant.now().minus(notificationProperties.getSchedule().getStaleLockThreshold());
        notificationScheduleJobRepository.recoverStaleLocks(staleBefore);

        int batchSize = Math.max(1, notificationProperties.getSchedule().getBatchSize());
        for (int i = 0; i < batchSize; i++) {
            NotificationScheduleJob job = notificationScheduleJobRepository
                    .findNextClaimableForUpdate()
                    .orElse(null);

            if (job == null) {
                return;
            }

            job.claim(workerId, Instant.now());
            processClaimedJob(job);
        }
    }

    @Transactional
    public void cleanupRetention() {
        if (!notificationProperties.getRetention().isEnabled()) {
            return;
        }

        NotificationProperties.Retention retention = notificationProperties.getRetention();
        Instant now = Instant.now();

        int notificationsCleaned = notificationRepository.cleanupExpired(
                now.minus(retention.getNotificationDeletedRetention()),
                now.minus(retention.getNotificationCreatedRetention())
        );

        int outboxCleaned = notificationOutboxService.cleanupSentAndDead(retention.getOutboxRetention());

        int scheduleCleaned = notificationScheduleJobRepository.cleanupDoneAndDead(
                now.minus(retention.getScheduleRetention())
        );

        int tokenCleaned = notificationTokenRepository.cleanupInactive(now.minus(retention.getInactiveTokenRetention()));

        log.info("Notification retention cleanup done. notifications={}, outbox={}, schedule={}, token={}",
                notificationsCleaned, outboxCleaned, scheduleCleaned, tokenCleaned);
    }

    private void processClaimedJob(NotificationScheduleJob job) {
        Instant now = Instant.now();

        try {
            if (job.getJobType() != NotificationJobType.REVIEW_REQUEST) {
                throw new IllegalStateException("unsupported job type: " + job.getJobType());
            }

            Long meetingId = job.getTargetId();
            Long memberId = job.getMember().getId();
            boolean activeParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                    meetingId,
                    memberId,
                    MeetingParticipant.Status.ACTIVE
            );
            if (!activeParticipant) {
                job.markDone();
                return;
            }
            boolean alreadyReviewed = reviewRepository.existsActiveReview(meetingId, memberId);

            if (!alreadyReviewed) {
                String eventKey = "REVIEW_REQUEST:" + meetingId + ":" + memberId + ":" + job.getId();
                notificationCommandService.createNotifications(
                        NotificationType.REVIEW_REQUEST,
                        "리뷰 작성 요청",
                        "식사 후기를 남겨주세요.",
                        "MEETING",
                        meetingId,
                        null,
                        "/meetings/" + meetingId + "/reviews",
                        eventKey,
                        null,
                        List.of(memberId)
                );
            }

            job.markDone();
        } catch (Exception ex) {
            if (job.hasReachedAttemptLimit(notificationProperties.getRetry().getMaxAttempts())) {
                job.markDead(safeMessage(ex), now);
                return;
            }

            Duration delay = NotificationRetryPolicy.backoffForAttempt(job.getAttemptCount());
            if (delay == null) {
                job.markDead(safeMessage(ex), now);
                return;
            }

            job.markFailed(safeMessage(ex), now.plus(delay));
        }
    }

    private String safeMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "schedule job failed";
        }
        return ex.getMessage();
    }
}
