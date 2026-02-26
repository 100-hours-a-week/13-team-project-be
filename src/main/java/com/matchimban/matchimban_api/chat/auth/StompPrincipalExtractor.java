package com.matchimban.matchimban_api.chat.auth;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import java.security.Principal;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class StompPrincipalExtractor {

	public MemberPrincipal extract(Principal principal) {
		if (principal instanceof AbstractAuthenticationToken authentication
			&& authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
			return memberPrincipal;
		}
		return null;
	}
}
