package com.matchimban.matchimban_api.notification.repository;

import com.matchimban.matchimban_api.notification.entity.NotificationScheduleJob;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationScheduleJobRepository extends JpaRepository<NotificationScheduleJob, Long> {

    @Query(value = """
        select *
        from notification_schedule_jobs
        where status in ('PENDING', 'FAILED')
          and run_at <= now()
          and next_attempt_at <= now()
        order by run_at asc, next_attempt_at asc, id asc
        for update skip locked
        limit 1
    """, nativeQuery = true)
    Optional<NotificationScheduleJob> findNextClaimableForUpdate();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update notification_schedule_jobs
           set status = 'FAILED',
               next_attempt_at = now(),
               locked_at = null,
               worker_id = null,
               last_error_message = 'stale lock recovered',
               updated_at = now()
         where status = 'IN_PROGRESS'
           and locked_at < :staleBefore
    """, nativeQuery = true)
    int recoverStaleLocks(@Param("staleBefore") Instant staleBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        delete from notification_schedule_jobs
         where status in ('DONE', 'DEAD')
           and updated_at < :cutoff
    """, nativeQuery = true)
    int cleanupDoneAndDead(@Param("cutoff") Instant cutoff);
}
