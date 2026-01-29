package com.matchimban.matchimban_api.vote.entity;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "vote_submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class VoteSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vote_submissions_seq_gen")
    @SequenceGenerator(name = "vote_submissions_seq_gen", sequenceName = "vote_submissions_seq", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    private VoteChoice choice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private MeetingParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_restaurant_id")
    private MeetingRestaurantCandidate candidateRestaurant;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
