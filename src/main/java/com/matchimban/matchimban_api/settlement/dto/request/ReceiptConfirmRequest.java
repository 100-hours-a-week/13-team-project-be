package com.matchimban.matchimban_api.settlement.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReceiptConfirmRequest(
        @NotBlank String objectKey
) {}