package com.matchimban.matchimban_api.global.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class SqlPerfLoggingService {

	private static final Logger LOG = LoggerFactory.getLogger("sql.perf");

	private final PerfLogProperties perfLogProperties;
	private final ObjectMapper objectMapper;

	public SqlPerfLoggingService(
		PerfLogProperties perfLogProperties,
		ObjectMapper objectMapper
	) {
		this.perfLogProperties = perfLogProperties;
		this.objectMapper = objectMapper;
	}

	public void logSlowQueries(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		if (!perfLogProperties.isEnabled() || !perfLogProperties.isLogSqlText()) {
			return;
		}

		long elapsedMs = (execInfo == null) ? 0L : Math.max(execInfo.getElapsedTime(), 0L);
		if (!perfLogProperties.isLogAllSql() && elapsedMs < perfLogProperties.getSlowQueryMs()) {
			return;
		}

		int queryCount = (queryInfoList == null) ? 0 : queryInfoList.size();
		if (queryCount == 0) {
			emitSqlPerfLog(elapsedMs, 0, "(unknown)");
			return;
		}

		for (QueryInfo queryInfo : queryInfoList) {
			String rawQuery = (queryInfo == null) ? null : queryInfo.getQuery();
			String querySample = normalizeSql(rawQuery);
			emitSqlPerfLog(elapsedMs, queryCount, querySample);
		}
	}

	private void emitSqlPerfLog(long elapsedMs, int queryCount, String querySample) {
		Map<String, Object> fields = new LinkedHashMap<>();
		fields.put("type", "sql_perf");
		fields.put("elapsedMs", elapsedMs);
		fields.put("queryCount", queryCount);
		fields.put("querySample", querySample);
		fields.put("queryHash", hash(querySample));

		String traceId = MDC.get("traceId");
		if (traceId != null && !traceId.isBlank()) {
			fields.put("traceId", traceId);
		}

		try {
			LOG.info(objectMapper.writeValueAsString(fields));
		} catch (JsonProcessingException ex) {
			LOG.info(
				"type=sql_perf elapsedMs={} queryCount={} queryHash={} querySample={}",
				elapsedMs,
				queryCount,
				hash(querySample),
				querySample
			);
		}
	}

	private String normalizeSql(String rawQuery) {
		if (rawQuery == null || rawQuery.isBlank()) {
			return "(blank)";
		}

		String normalized = rawQuery.replaceAll("\\s+", " ").trim();
		int maxLength = Math.max(perfLogProperties.getMaxSqlLength(), 100);
		if (normalized.length() <= maxLength) {
			return normalized;
		}
		return normalized.substring(0, maxLength) + "...(truncated)";
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return toHex(hash).substring(0, 16);
		} catch (NoSuchAlgorithmException ex) {
			return Integer.toHexString(value.hashCode());
		}
	}

	private String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
