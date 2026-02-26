package com.matchimban.matchimban_api.settlement.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RunpodOcrResponse(
        @JsonProperty("request_id") String requestId,
        ReceiptResult result,
        ErrorDetail error
) {
    public record ReceiptResult(
            List<ReceiptItem> items,
            @JsonProperty("total_amount") double totalAmount,
            @JsonProperty("discount_amount") double discountAmount,
            @JsonProperty("paid_amount") double paidAmount,
            @JsonProperty("created_at") String createdAt
    ) {}

    public record ReceiptItem(
            String name,
            @JsonProperty("unit_price") double unitPrice,
            double quantity,
            double amount
    ) {}

    public record ErrorDetail(
            String code,
            String message
    ) {}
}