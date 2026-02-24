package com.matchimban.matchimban_api.chat.event;

public record ChatUnreadCountsRefreshInternalEvent(
	Long meetingId
) {
}
