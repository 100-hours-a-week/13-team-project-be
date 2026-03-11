package com.matchimban.matchimban_api.settlement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MenuSelectionConfirmRequest(
        @NotNull List<@NotNull @Positive Long> selectedItemIds
) {}