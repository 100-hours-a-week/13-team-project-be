package com.matchimban.matchimban_api.chat.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.chat.metrics.ChatMetricsRecorder;
import com.matchimban.matchimban_api.chat.repository.projection.ChatMessageRow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageCacheService {

	private static final String RECENT_MESSAGES_VERSION_KEY_PREFIX = "chat:meeting:messages:version:";
	private static final String RECENT_MESSAGES_VERSIONED_KEY_PREFIX = "chat:meeting:messages:recent:";
	private static final String RECENT_MESSAGES_TRAFFIC_KEY_PREFIX = "chat:meeting:messages:traffic:";
	private static final String LATEST_MESSAGE_ID_KEY_PREFIX = "chat:meeting:latest-message-id:";
	private static final String RECENT_MESSAGES_LOCK_KEY_PREFIX = "chat:meeting:messages:lock:";
	private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
		"if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
		Long.class
	);

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final ChatMetricsRecorder chatMetricsRecorder;

	@Value("${chat.cache.messages.recent.enabled:true}")
	private boolean enabled;

	@Value("${chat.cache.messages.recent.window-size:100}")
	private int recentWindowSize;

	@Value("${chat.cache.messages.recent.ttl-seconds:120}")
	private long recentTtlSeconds;

	@Value("${chat.cache.messages.recent.hot-ttl-seconds:30}")
	private long recentHotTtlSeconds;

	@Value("${chat.cache.messages.recent.cold-ttl-seconds:180}")
	private long recentColdTtlSeconds;

	@Value("${chat.cache.messages.recent.only-hot-rooms:false}")
	private boolean cacheOnlyHotRooms;

	@Value("${chat.cache.messages.recent.hot-threshold-per-window:120}")
	private long hotThresholdPerWindow;

	@Value("${chat.cache.messages.recent.traffic-window-seconds:60}")
	private long trafficWindowSeconds;

	@Value("${chat.cache.messages.latest-id.ttl-seconds:120}")
	private long latestMessageIdTtlSeconds;

	@Value("${chat.cache.messages.recent.lock-enabled:true}")
	private boolean lockEnabled;

	@Value("${chat.cache.messages.recent.lock-ttl-millis:2000}")
	private long lockTtlMillis;

	@Value("${chat.cache.messages.recent.lock-retry-count:15}")
	private int lockRetryCount;

	@Value("${chat.cache.messages.recent.lock-retry-backoff-millis:5}")
	private long lockRetryBackoffMillis;

	public boolean isEnabled() {
		return enabled;
	}

	public int recentWindowSize() {
		return recentWindowSize;
	}

	public boolean recordTrafficAndIsCacheEligible(Long meetingId) {
		if (!enabled || meetingId == null) {
			return false;
		}
		long traffic = incrementTrafficCounter(meetingId);
		return !cacheOnlyHotRooms || traffic >= hotThresholdPerWindow;
	}

	public Optional<List<ChatMessageRow>> getRecentMessages(Long meetingId, int fetchSize) {
		if (!enabled || meetingId == null || fetchSize <= 0) {
			return Optional.empty();
		}

		Optional<Long> currentVersion = getCurrentVersion(meetingId);
		if (currentVersion.isEmpty()) {
			chatMetricsRecorder.recordMessageRecentCacheLookupMiss();
			return Optional.empty();
		}
		String key = recentMessagesVersionedKey(meetingId, currentVersion.get());
		try {
			List<String> payloads = stringRedisTemplate.opsForList().range(key, 0, fetchSize - 1L);
			if (payloads == null || payloads.isEmpty()) {
				chatMetricsRecorder.recordMessageRecentCacheLookupMiss();
				return Optional.empty();
			}

			List<ChatMessageRow> rows = new ArrayList<>(payloads.size());
			for (String payload : payloads) {
				rows.add(objectMapper.readValue(payload, ChatMessageRow.class));
			}
			chatMetricsRecorder.recordMessageRecentCacheLookupHit();
			return Optional.of(rows);
		} catch (Exception ex) {
			chatMetricsRecorder.recordMessageRecentCacheLookupError();
			log.warn("Failed to read chat messages cache. meetingId={}", meetingId, ex);
			return Optional.empty();
		}
	}

	public void replaceRecentMessages(Long meetingId, List<ChatMessageRow> rowsDesc) {
		if (!enabled || meetingId == null || rowsDesc == null || rowsDesc.isEmpty()) {
			return;
		}

		withRoomLock(meetingId, () -> doReplaceRecentMessages(meetingId, rowsDesc));
	}

	public void appendRecentMessage(Long meetingId, ChatMessageRow row) {
		if (!enabled || meetingId == null || row == null || row.messageId() == null) {
			return;
		}

		withRoomLock(meetingId, () -> doAppendRecentMessage(meetingId, row));
	}

	private void doReplaceRecentMessages(Long meetingId, List<ChatMessageRow> rowsDesc) {
		try {
			Long previousVersion = getCurrentVersion(meetingId).orElse(null);
			Long nextVersion = stringRedisTemplate.opsForValue().increment(recentMessagesVersionKey(meetingId));
			if (nextVersion == null) {
				return;
			}
			persistRecentSnapshot(meetingId, nextVersion, rowsDesc, resolveRecentTtlSeconds(meetingId));
			deleteOlderVersionSnapshot(meetingId, previousVersion, nextVersion);
			cacheLatestMessageId(meetingId, rowsDesc.get(0).messageId());
		} catch (Exception ex) {
			log.warn("Failed to replace chat messages cache. meetingId={}", meetingId, ex);
		}
	}

	private void doAppendRecentMessage(Long meetingId, ChatMessageRow row) {
		try {
			Long previousVersion = getCurrentVersion(meetingId).orElse(null);
			List<ChatMessageRow> previous = loadPreviousSnapshot(meetingId, previousVersion);
			List<ChatMessageRow> merged = mergeLatest(previous, row);
			Long nextVersion = stringRedisTemplate.opsForValue().increment(recentMessagesVersionKey(meetingId));
			if (nextVersion == null) {
				return;
			}
			persistRecentSnapshot(meetingId, nextVersion, merged, resolveRecentTtlSeconds(meetingId));
			deleteOlderVersionSnapshot(meetingId, previousVersion, nextVersion);
			cacheLatestMessageId(meetingId, row.messageId());
		} catch (Exception ex) {
			log.warn("Failed to append chat message cache. meetingId={} messageId={}", meetingId, row.messageId(), ex);
		}
	}

	private void withRoomLock(Long meetingId, Runnable action) {
		if (!lockEnabled) {
			action.run();
			return;
		}

		String lockKey = recentMessagesLockKey(meetingId);
		String lockToken = UUID.randomUUID().toString();
		boolean acquired = tryAcquireLock(lockKey, lockToken);
		if (!acquired) {
			log.warn("Skipped chat cache update due to lock contention. meetingId={}", meetingId);
			return;
		}

		try {
			action.run();
		} finally {
			releaseLock(lockKey, lockToken);
		}
	}

	private boolean tryAcquireLock(String lockKey, String lockToken) {
		long ttl = Math.max(100L, lockTtlMillis);
		int retries = Math.max(0, lockRetryCount);
		long backoff = Math.max(1L, lockRetryBackoffMillis);

		for (int i = 0; i <= retries; i++) {
			Boolean acquired = stringRedisTemplate.opsForValue()
				.setIfAbsent(lockKey, lockToken, Duration.ofMillis(ttl));
			if (Boolean.TRUE.equals(acquired)) {
				return true;
			}
			if (i < retries) {
				sleepQuietly(backoff);
			}
		}
		return false;
	}

	private void releaseLock(String lockKey, String lockToken) {
		try {
			stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockToken);
		} catch (Exception ex) {
			log.warn("Failed to release chat cache lock. lockKey={}", lockKey, ex);
		}
	}

	private void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void cacheLatestMessageId(Long meetingId, String messageId) {
		if (messageId == null) {
			return;
		}
		String key = latestMessageIdKey(meetingId);
		stringRedisTemplate.opsForValue().set(
			key,
			messageId,
			Duration.ofSeconds(latestMessageIdTtlSeconds)
		);
	}

	private List<ChatMessageRow> loadPreviousSnapshot(Long meetingId, Long version) {
		if (version == null || version <= 0) {
			return List.of();
		}
		String key = recentMessagesVersionedKey(meetingId, version);
		try {
			List<String> payloads = stringRedisTemplate.opsForList().range(key, 0, recentWindowSize);
			if (payloads == null || payloads.isEmpty()) {
				return List.of();
			}
			return deserializeRows(payloads);
		} catch (Exception ex) {
			log.warn("Failed to load previous chat cache snapshot. meetingId={} version={}", meetingId, version, ex);
			return List.of();
		}
	}

	private List<ChatMessageRow> mergeLatest(List<ChatMessageRow> previous, ChatMessageRow latest) {
		List<ChatMessageRow> merged = new ArrayList<>(Math.max(1, previous.size() + 1));
		merged.add(latest);
		for (ChatMessageRow row : previous) {
			if (row.messageId() != null && row.messageId().equals(latest.messageId())) {
				continue;
			}
			merged.add(row);
			if (merged.size() >= recentWindowSize + 1) {
				break;
			}
		}
		return merged;
	}

	private void persistRecentSnapshot(Long meetingId, Long version, List<ChatMessageRow> rowsDesc, long ttlSeconds) throws Exception {
		List<String> payloads = serializeRows(rowsDesc);
		if (payloads.isEmpty()) {
			return;
		}
		String key = recentMessagesVersionedKey(meetingId, version);
		stringRedisTemplate.delete(key);
		stringRedisTemplate.opsForList().rightPushAll(key, payloads);
		long effectiveTtl = Math.max(1, ttlSeconds > 0 ? ttlSeconds : recentTtlSeconds);
		stringRedisTemplate.expire(key, Duration.ofSeconds(effectiveTtl));
	}

	private void deleteOlderVersionSnapshot(Long meetingId, Long previousVersion, Long currentVersion) {
		if (previousVersion == null || previousVersion <= 0 || currentVersion == null || previousVersion.equals(currentVersion)) {
			return;
		}
		stringRedisTemplate.delete(recentMessagesVersionedKey(meetingId, previousVersion));
	}

	private long resolveRecentTtlSeconds(Long meetingId) {
		long traffic = getTrafficCounter(meetingId);
		boolean isHot = traffic >= hotThresholdPerWindow;
		long hot = Math.max(1, recentHotTtlSeconds);
		long cold = Math.max(1, recentColdTtlSeconds);
		return isHot ? hot : cold;
	}

	private long incrementTrafficCounter(Long meetingId) {
		String key = recentMessagesTrafficKey(meetingId);
		Long value = stringRedisTemplate.opsForValue().increment(key);
		long window = Math.max(1, trafficWindowSeconds);
		stringRedisTemplate.expire(key, Duration.ofSeconds(window));
		return value == null ? 0L : value;
	}

	private long getTrafficCounter(Long meetingId) {
		String key = recentMessagesTrafficKey(meetingId);
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private Optional<Long> getCurrentVersion(Long meetingId) {
		String raw = stringRedisTemplate.opsForValue().get(recentMessagesVersionKey(meetingId));
		if (raw == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(raw));
		} catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	private List<ChatMessageRow> deserializeRows(List<String> payloads) throws Exception {
		List<ChatMessageRow> rows = new ArrayList<>(payloads.size());
		for (String payload : payloads) {
			rows.add(objectMapper.readValue(payload, ChatMessageRow.class));
		}
		return rows;
	}

	private List<String> serializeRows(List<ChatMessageRow> rows) throws Exception {
		List<ChatMessageRow> limitedRows = rows.stream()
			.limit((long) recentWindowSize + 1L)
			.collect(Collectors.toList());
		List<String> payloads = new ArrayList<>(rows.size());
		for (ChatMessageRow row : limitedRows) {
			payloads.add(objectMapper.writeValueAsString(row));
		}
		return payloads;
	}

	private String recentMessagesVersionKey(Long meetingId) {
		return RECENT_MESSAGES_VERSION_KEY_PREFIX + meetingId;
	}

	private String recentMessagesVersionedKey(Long meetingId, Long version) {
		return RECENT_MESSAGES_VERSIONED_KEY_PREFIX + meetingId + ":v:" + version;
	}

	private String recentMessagesTrafficKey(Long meetingId) {
		return RECENT_MESSAGES_TRAFFIC_KEY_PREFIX + meetingId;
	}

	private String latestMessageIdKey(Long meetingId) {
		return LATEST_MESSAGE_ID_KEY_PREFIX + meetingId;
	}

	private String recentMessagesLockKey(Long meetingId) {
		return RECENT_MESSAGES_LOCK_KEY_PREFIX + meetingId;
	}
}
