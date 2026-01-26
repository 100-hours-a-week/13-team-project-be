package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.MemberCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCategoryMappingRepository extends JpaRepository<MemberCategoryMapping, Long> {
	void deleteByMemberId(Long memberId);
}
