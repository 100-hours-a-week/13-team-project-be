package com.matchimban.matchimban_api.vote.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FinalSelectionResponse {

    private Long candidateId;
    private Long restaurantId;

    private String restaurantName;
    private String imageUrl1;
    private String imageUrl2;
    private String imageUrl3;

    private String categoryName;
    private BigDecimal rating;

    private Integer distanceM;

    private String roadAddress;
    private String jibunAddress;
}
