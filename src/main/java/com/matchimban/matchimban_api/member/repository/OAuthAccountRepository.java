package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.OAuthAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
	Optional<OAuthAccount> findByProviderAndProviderMemberId(String provider, String providerMemberId);
}
