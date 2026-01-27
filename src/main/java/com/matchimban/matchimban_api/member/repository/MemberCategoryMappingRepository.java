package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.MemberCategoryMapping;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberCategoryMappingRepository extends JpaRepository<MemberCategoryMapping, Long> {
	void deleteByMemberId(Long memberId);

	@Query("""
		select m
		from MemberCategoryMapping m
		join fetch m.category
		where m.member.id = :memberId
		""")
	List<MemberCategoryMapping> findByMemberIdWithCategory(@Param("memberId") Long memberId);
}
