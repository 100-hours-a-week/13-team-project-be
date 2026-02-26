package com.matchimban.matchimban_api.settlement.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RunpodOcrRequest(
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("request_id") String requestId
) {}