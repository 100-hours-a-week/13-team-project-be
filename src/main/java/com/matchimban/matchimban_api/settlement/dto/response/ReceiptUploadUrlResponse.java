package com.matchimban.matchimban_api.settlement.dto.response;

import java.time.Instant;

public record ReceiptUploadUrlResponse(
        String objectKey,
        String uploadUrl,
        Instant expiresAt
) {}