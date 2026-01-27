package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.MemberAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {
	List<MemberAgreement> findByMemberId(Long memberId);
	boolean existsByMemberIdAndPolicyId(Long memberId, Long policyId);
}
