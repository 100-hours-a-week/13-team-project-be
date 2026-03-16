package com.matchimban.matchimban_api.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationTokenUpsertRequest(
        @NotBlank @Size(max = 4096) String fcmToken,
        @Size(max = 120) String deviceKey,
        @Size(max = 500) String userAgent
) {
}
