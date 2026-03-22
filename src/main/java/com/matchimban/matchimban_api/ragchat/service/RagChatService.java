package com.matchimban.matchimban_api.ragchat.service;

import com.matchimban.matchimban_api.ragchat.dto.request.RagChatAskRequest;
import com.matchimban.matchimban_api.ragchat.dto.response.RagChatAskData;
import com.matchimban.matchimban_api.ragchat.dto.response.RagHealthData;
import com.matchimban.matchimban_api.ragchat.dto.response.RagHistoryData;

public interface RagChatService {

	RagChatAskData ask(Long memberId, RagChatAskRequest request);

	void resetHistory(Long memberId, String userId);

	RagHistoryData getHistory(Long memberId, String userId, int limit, Long beforeId);

	RagHealthData health();

	void assertOwner(Long memberId, String userId);
}
