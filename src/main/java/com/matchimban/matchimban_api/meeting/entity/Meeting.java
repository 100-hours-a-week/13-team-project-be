package com.matchimban.matchimban_api.meeting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(name = "meetings")
@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meetings_seq_gen")
    @SequenceGenerator(
            name = "meetings_seq_gen",
            sequenceName = "meetings_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(length = 20, nullable = false)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "location_address", length = 255, nullable = false)
    private String locationAddress;

    @Column(name="location_lat", nullable=false, precision=10, scale=7)
    private BigDecimal locationLat;

    @Column(name="location_lng", nullable=false, precision=10, scale=7)
    private BigDecimal locationLng;


    @Column(name = "target_headcount", nullable = false)
    private int targetHeadcount;

    @Column(name = "search_radius_m", nullable = false)
    private int searchRadiusM;

    @Column(name = "vote_deadline_at", nullable = false)
    private LocalDateTime voteDeadlineAt;

    @Column(name = "is_except_meat", nullable = false)
    private boolean isExceptMeat;

    @Column(name = "is_except_bar", nullable = false)
    private boolean isExceptBar;

    @Column(name = "swipe_count", nullable = false)
    private int swipeCount;

    @Builder.Default
    @Column(name = "is_quick_meeting", nullable = false)
    private boolean isQuickMeeting = false;

    @Column(name = "invite_code", length = 8, nullable = false)
    private String inviteCode;

    @Column(name = "last_chat_id")
    private Long lastChatId;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;
}
