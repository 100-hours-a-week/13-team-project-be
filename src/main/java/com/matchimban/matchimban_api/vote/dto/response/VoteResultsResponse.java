package com.matchimban.matchimban_api.vote.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoteResultsResponse {

    private List<Item> items;

    @Getter
    @AllArgsConstructor
    public static class Item {
        private Long candidateId;
        private Integer rank;

        private String restaurantName;
        private String imageUrl1;
        private String categoryName;
        private BigDecimal rating;

        private Integer likeCount;
        private Integer distanceM;

        private String roadAddress;
        private String jibunAddress;
    }
}
