package com.matchimban.matchimban_api.settlement.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReceiptUploadUrlRequest(@NotBlank String contentType) {}