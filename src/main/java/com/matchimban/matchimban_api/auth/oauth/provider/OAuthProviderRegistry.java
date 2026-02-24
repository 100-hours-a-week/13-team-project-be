package com.matchimban.matchimban_api.auth.oauth.provider;

import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Registry 역할: OAuthProviderType -> OAuthProvider(Strategy) 매핑.
 * - 컨트롤러/서비스는 "provider를 선택"만 하고, 구체 구현(Kakao/Google/...)은 숨긴다.
 * - Provider 확장 시, 구현체(@Component/@Service)를 추가하면 자동 등록된다.
 */
@Component
public final class OAuthProviderRegistry {

	private final Map<OAuthProviderType, OAuthProvider> providers;

	public OAuthProviderRegistry(List<OAuthProvider> providers) {
		Map<OAuthProviderType, OAuthProvider> map = new EnumMap<>(OAuthProviderType.class);
		for (OAuthProvider provider : providers) {
			OAuthProviderType type = provider.type();
			if (map.containsKey(type)) {
				throw new IllegalStateException("Duplicate OAuthProvider for type: " + type);
			}
			map.put(type, provider);
		}
		this.providers = Map.copyOf(map);
	}

	public OAuthProvider get(OAuthProviderType type) {
		return find(type)
			.orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider: " + type));
	}

	public Optional<OAuthProvider> find(OAuthProviderType type) {
		if (type == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(providers.get(type));
	}
}
