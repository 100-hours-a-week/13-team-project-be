package com.matchimban.matchimban_api.settlement.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MenuSelectionConfirmRequest(
        @NotEmpty List<Long> selectedItemIds
) {}