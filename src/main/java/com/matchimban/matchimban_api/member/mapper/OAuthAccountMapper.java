package com.matchimban.matchimban_api.member.mapper;

import com.matchimban.matchimban_api.member.dto.OAuthAccountCreateRequest;
import com.matchimban.matchimban_api.member.entity.OAuthAccount;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class OAuthAccountMapper {

	public OAuthAccount toOAuthAccount(OAuthAccountCreateRequest request) {
		return OAuthAccount.builder()
			.provider(request.provider())
			.providerMemberId(request.providerMemberId())
			.member(request.member())
			.createdAt(LocalDateTime.now())
			.build();
	}
}
