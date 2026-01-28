package com.matchimban.matchimban_api.vote.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoteCandidatesResponse {

    private List<Candidate> candidates;

    @Getter
    @AllArgsConstructor
    public static class Candidate {
        private Long candidateId;

        private String restaurantName;
        private String imageUrl1;
        private String imageUrl2;
        private String imageUrl3;

        private Integer distanceM;
        private BigDecimal rating;

        private String categoryName;

        private String roadAddress;
        private String jibunAddress;
    }
}
