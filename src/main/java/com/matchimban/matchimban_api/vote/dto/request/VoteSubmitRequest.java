package com.matchimban.matchimban_api.vote.dto.request;

import com.matchimban.matchimban_api.vote.entity.VoteChoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VoteSubmitRequest {

    @Valid
    @NotEmpty
    private List<Item> items;

    @Getter
    @NoArgsConstructor
    public static class Item {
        @NotNull
        private Long candidateId;

        @NotNull
        private VoteChoice choice;
    }
}
