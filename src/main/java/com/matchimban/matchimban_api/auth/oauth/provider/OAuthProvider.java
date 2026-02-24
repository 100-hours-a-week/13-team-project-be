package com.matchimban.matchimban_api.auth.oauth.provider;

import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthToken;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthUserInfo;

public interface OAuthProvider {

	OAuthProviderType type();

	String buildAuthorizeUrl(String state);

	OAuthToken requestToken(String code);

	OAuthUserInfo requestUserInfo(String accessToken);

	void unlink(String providerMemberId);

	String frontendRedirectUrl();
}

