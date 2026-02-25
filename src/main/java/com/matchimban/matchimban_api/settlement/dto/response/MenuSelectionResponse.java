package com.matchimban.matchimban_api.settlement.dto.response;

import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;

import java.math.BigDecimal;
import java.util.List;

public record MenuSelectionResponse(
        Long settlementId,
        SettlementStatus settlementStatus,
        boolean mySelectionConfirmed,
        List<Long> mySelectedItemIds,
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