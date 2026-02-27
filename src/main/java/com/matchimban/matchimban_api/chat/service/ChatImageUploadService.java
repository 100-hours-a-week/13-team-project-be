package com.matchimban.matchimban_api.chat.service;

import com.matchimban.matchimban_api.chat.dto.http.ChatImagePresignData;
import com.matchimban.matchimban_api.chat.dto.http.ChatImagePresignRequest;

public interface ChatImageUploadService {

	ChatImagePresignData issuePresignedUpload(
		Long memberId,
		Long meetingId,
		ChatImagePresignRequest request
	);
}

