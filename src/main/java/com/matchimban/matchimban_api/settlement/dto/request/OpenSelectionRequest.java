package com.matchimban.matchimban_api.settlement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record OpenSelectionRequest(
        BigDecimal totalAmount,
        BigDecimal discountAmount,

        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            Long itemId,
            @NotNull String name,
            BigDecimal unitPrice,
            Integer quantity,
            @NotNull BigDecimal totalPrice
    ) {}
}