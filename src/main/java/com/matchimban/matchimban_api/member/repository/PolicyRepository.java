package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.Policy;
import com.matchimban.matchimban_api.member.entity.enums.PolicyType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
	Optional<Policy> findByPolicyTypeAndTermsVersion(PolicyType policyType, String termsVersion);
	List<Policy> findByIsRequiredTrue();
}
