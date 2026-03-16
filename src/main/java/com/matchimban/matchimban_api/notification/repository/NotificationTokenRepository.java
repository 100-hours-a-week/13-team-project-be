package com.matchimban.matchimban_api.notification.repository;

import com.matchimban.matchimban_api.notification.entity.NotificationToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {

    Optional<NotificationToken> findByFcmToken(String fcmToken);

    Optional<NotificationToken> findByMemberIdAndFcmToken(Long memberId, String fcmToken);

    Optional<NotificationToken> findFirstByMemberIdAndIsActiveTrueOrderByUpdatedAtDesc(Long memberId);

    List<NotificationToken> findAllByMemberIdAndDeviceKeyAndIsActiveTrue(Long memberId, String deviceKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from NotificationToken t
        where t.isActive = false
          and t.updatedAt < :cutoff
    """)
    int cleanupInactive(@Param("cutoff") Instant cutoff);
}
