package com.matchimban.matchimban_api.global.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiPerfLoggingFilter extends OncePerRequestFilter {

	private static final Logger LOG = LoggerFactory.getLogger("api.perf");

	private final PerfLogProperties perfLogProperties;
	private final ObjectMapper objectMapper;

	public ApiPerfLoggingFilter(
		PerfLogProperties perfLogProperties,
		ObjectMapper objectMapper
	) {
		this.perfLogProperties = perfLogProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!perfLogProperties.isEnabled()) {
			return true;
		}
		String path = request.getRequestURI();
		List<String> excluded = perfLogProperties.getExcludePathPrefixes();
		if (excluded == null || excluded.isEmpty()) {
			return false;
		}
		for (String prefix : excluded) {
			if (prefix != null && !prefix.isBlank() && path.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		long startNs = System.nanoTime();
		RequestSqlMetricsContext.start();
		Throwable throwable = null;
		try {
			filterChain.doFilter(request, response);
		} catch (Throwable ex) {
			throwable = ex;
			throw ex;
		} finally {
			long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
			RequestSqlMetricsContext.SqlMetrics sqlMetrics = RequestSqlMetricsContext.snapshot();
			RequestSqlMetricsContext.clear();

			int status = resolveStatus(response, throwable);
			boolean slowRequest = elapsedMs >= perfLogProperties.getSlowRequestMs();
			boolean slowQuery = sqlMetrics.queryTimeMs() >= perfLogProperties.getSlowQueryMs();
			boolean serverError = status >= HttpStatus.INTERNAL_SERVER_ERROR.value();

			if (slowRequest || slowQuery || serverError) {
				logAsJson(request, status, elapsedMs, sqlMetrics, slowRequest, slowQuery, serverError);
			}
		}
	}

	private int resolveStatus(HttpServletResponse response, Throwable throwable) {
		if (throwable != null && response.getStatus() < HttpStatus.BAD_REQUEST.value()) {
			return HttpStatus.INTERNAL_SERVER_ERROR.value();
		}
		return response.getStatus();
	}

	private void logAsJson(
		HttpServletRequest request,
		int status,
		long elapsedMs,
		RequestSqlMetricsContext.SqlMetrics sqlMetrics,
		boolean slowRequest,
		boolean slowQuery,
		boolean serverError
	) {
		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("type", "api_perf");
		fields.put("method", request.getMethod());
		fields.put("path", request.getRequestURI());
		fields.put("status", status);
		fields.put("elapsedMs", elapsedMs);
		fields.put("queryCount", sqlMetrics.queryCount());
		fields.put("queryTimeMs", sqlMetrics.queryTimeMs());
		fields.put("slowRequest", slowRequest);
		fields.put("slowQuery", slowQuery);
		fields.put("serverError", serverError);
		fields.put("clientIp", resolveClientIp(request));

		String traceId = MDC.get("traceId");
		if (traceId != null && !traceId.isBlank()) {
			fields.put("traceId", traceId);
		}

		String requestId = request.getHeader("X-Request-Id");
		if (requestId != null && !requestId.isBlank()) {
			fields.put("requestId", requestId);
		}

		try {
			LOG.info(objectMapper.writeValueAsString(fields));
		} catch (JsonProcessingException ex) {
			LOG.info(
				"type=api_perf method={} path={} status={} elapsedMs={} queryCount={} queryTimeMs={}",
				request.getMethod(),
				request.getRequestURI(),
				status,
				elapsedMs,
				sqlMetrics.queryCount(),
				sqlMetrics.queryTimeMs()
			);
		}
	}

	private String resolveClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor == null || xForwardedFor.isBlank()) {
			return request.getRemoteAddr();
		}
		String[] ips = xForwardedFor.split(",");
		return ips.length == 0 ? request.getRemoteAddr() : ips[0].trim();
	}
}

