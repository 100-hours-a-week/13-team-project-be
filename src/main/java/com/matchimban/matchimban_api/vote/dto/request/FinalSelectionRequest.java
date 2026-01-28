package com.matchimban.matchimban_api.vote.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FinalSelectionRequest {
    @NotNull
    private Long candidateId;
}
