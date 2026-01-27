package com.matchimban.matchimban_api.vote.entity;

import com.matchimban.matchimban_api.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_final_selections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MeetingFinalSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_final_selections_seq_gen")
    @SequenceGenerator(
            name = "meeting_final_selections_seq_gen",
            sequenceName = "meeting_final_selections_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_candidate_id")
    private MeetingRestaurantCandidate finalCandidate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
