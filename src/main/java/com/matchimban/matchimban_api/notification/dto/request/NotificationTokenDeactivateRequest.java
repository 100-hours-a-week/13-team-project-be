package com.matchimban.matchimban_api.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationTokenDeactivateRequest(
        @NotBlank @Size(max = 4096) String fcmToken
) {
}
