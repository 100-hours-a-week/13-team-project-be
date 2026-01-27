package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
