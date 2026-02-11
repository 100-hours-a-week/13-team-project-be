package com.matchimban.matchimban_api.global.logging;

public final class RequestSqlMetricsContext {

	private static final ThreadLocal<MetricsAccumulator> HOLDER = new ThreadLocal<>();

	private RequestSqlMetricsContext() {
	}

	public static void start() {
		HOLDER.set(new MetricsAccumulator());
	}

	public static void addQueryMetrics(int queryCount, long elapsedMs) {
		MetricsAccumulator accumulator = HOLDER.get();
		if (accumulator == null) {
			return;
		}
		accumulator.queryCount += Math.max(queryCount, 0);
		accumulator.queryTimeMs += Math.max(elapsedMs, 0L);
	}

	public static SqlMetrics snapshot() {
		MetricsAccumulator accumulator = HOLDER.get();
		if (accumulator == null) {
			return new SqlMetrics(0, 0L);
		}
		return new SqlMetrics(accumulator.queryCount, accumulator.queryTimeMs);
	}

	public static void clear() {
		HOLDER.remove();
	}

	public record SqlMetrics(
		int queryCount,
		long queryTimeMs
	) {
	}

	private static final class MetricsAccumulator {
		private int queryCount;
		private long queryTimeMs;
	}
}

