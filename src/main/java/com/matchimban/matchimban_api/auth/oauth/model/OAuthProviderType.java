package com.matchimban.matchimban_api.auth.oauth.model;

import java.util.Locale;

public enum OAuthProviderType {
	KAKAO;

	public String provider() {
		// DB에 저장되는 provider 값은 기존과 동일하게 유지한다. (ex. "KAKAO")
		return name();
	}

	public static OAuthProviderType fromProvider(String provider) {
		if (provider == null || provider.isBlank()) {
			throw new IllegalArgumentException("provider is required");
		}
		return OAuthProviderType.valueOf(provider.trim().toUpperCase(Locale.ROOT));
	}
}

