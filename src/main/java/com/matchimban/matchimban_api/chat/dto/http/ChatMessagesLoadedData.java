package com.matchimban.matchimban_api.chat.dto.http;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "메시지 목록 조회 결과")
public record ChatMessagesLoadedData(
	List<ChatMessageItemDto> items,
	ChatMessagePageDto page
) {
}
