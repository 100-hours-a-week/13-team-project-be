package com.matchimban.matchimban_api.settlement.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SettlementItemsResponse(
        Long settlementId,
        String receiptImageUrl,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        List<Item> items
) {
    public record Item(
            Long itemId,
            String name,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalPrice
    ) {}
}