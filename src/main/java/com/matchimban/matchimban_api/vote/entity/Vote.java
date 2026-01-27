package com.matchimban.matchimban_api.vote.entity;

import com.matchimban.matchimban_api.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "votes_seq_gen")
    @SequenceGenerator(name = "votes_seq_gen", sequenceName = "votes_seq", allocationSize = 50)
    private Long id;

    private short round;

    @Enumerated(EnumType.STRING)
    private VoteStatus status;

    private LocalDateTime generatedAt;
    private LocalDateTime countedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
