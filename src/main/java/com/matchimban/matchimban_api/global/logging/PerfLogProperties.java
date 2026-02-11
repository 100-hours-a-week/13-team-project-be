package com.matchimban.matchimban_api.global.logging;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "perf-log")
public class PerfLogProperties {

	private boolean enabled = true;
	private long slowRequestMs = 500L;
	private long slowQueryMs = 200L;
	private List<String> excludePathPrefixes = List.of("/actuator", "/swagger-ui", "/v3/api-docs");

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public long getSlowRequestMs() {
		return slowRequestMs;
	}

	public void setSlowRequestMs(long slowRequestMs) {
		this.slowRequestMs = slowRequestMs;
	}

	public long getSlowQueryMs() {
		return slowQueryMs;
	}

	public void setSlowQueryMs(long slowQueryMs) {
		this.slowQueryMs = slowQueryMs;
	}

	public List<String> getExcludePathPrefixes() {
		return excludePathPrefixes;
	}

	public void setExcludePathPrefixes(List<String> excludePathPrefixes) {
		this.excludePathPrefixes = excludePathPrefixes;
	}
}

