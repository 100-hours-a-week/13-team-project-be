package com.matchimban.matchimban_api.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Table(name = "users")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq_gen")
    @SequenceGenerator(
            name = "users_seq_gen",
            sequenceName = "users_seq",
            allocationSize = 50
    )
    private Long id;
}
