package com.matchimban.matchimban_api.global.logging;

import java.util.List;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class SqlMetricsQueryListener implements QueryExecutionListener {

	@Override
	public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		// no-op
	}

	@Override
	public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		int queryCount = (queryInfoList == null) ? 0 : queryInfoList.size();
		long elapsedMs = (execInfo == null) ? 0L : execInfo.getElapsedTime();
		RequestSqlMetricsContext.addQueryMetrics(queryCount, elapsedMs);
	}
}

