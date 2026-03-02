package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByGuestUuid(UUID guestUuid);
}
