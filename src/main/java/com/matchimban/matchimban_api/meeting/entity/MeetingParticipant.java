package com.matchimban.matchimban_api.meeting.entity;

import com.matchimban.matchimban_api.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MeetingParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_participants_seq_gen")
    @SequenceGenerator(
            name = "meeting_participants_seq_gen",
            sequenceName = "meeting_participants_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Status status;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private MeetingParticipant(Meeting meeting, Member member, Role role, Status status) {
        this.meeting = meeting;
        this.member = member;
        this.role = role;
        this.status = status;
    }

    public void reactivate() {
        this.status = Status.ACTIVE;
    }

    public enum Role {
        HOST,
        MEMBER;
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        LEFT;
    }

    public void leave() {
        this.status = Status.LEFT;
    }
}
