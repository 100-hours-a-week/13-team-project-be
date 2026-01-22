package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateMeetingRequest {

    @Schema(description = "모임 이름")
    @NotBlank
    @Size(min=2, max=20)
    @Pattern(regexp = "^[가-힣A-Za-z0-9\\s\\-_.!,?()\\[\\]]{2,20}$")
    private String title;

    @Schema(description = "모임 일시")
    @NotNull
    private LocalDateTime scheduledAt;

    @Schema(description = "모임 주소")
    @NotBlank
    @Size(max = 255)
    private String locationAddress;

    @Schema(description = "모임 장소 위도", example = "37.498095")
    @NotNull
    @Digits(integer = 3, fraction = 7)
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal locationLat;

    @Schema(description = "모임 장소 경도", example = "127.027610")
    @NotNull
    @Digits(integer = 3, fraction = 7)
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal locationLng;

    @Schema(description = "목표 인원")
    @Min(2)
    private int targetHeadcount;

    @Schema(description = "탐색 범위")
    @Min(1)
    @Max(3000)
    private int searchRadiusM;

    @Schema(description = "투표 마감 일시")
    @NotNull
    private LocalDateTime voteDeadlineAt;

    @Schema(description = "고깃집 제외 여부")
    private boolean isExceptMeat;

    @Schema(description = "술집 제외 여부")
    private boolean isExceptBar;

    @Schema(description = "스와이프 수")
    @Min(1)
    @Max(15)
    private int swipeCount;

    @Schema(description = "퀵 모임 여부")
    private boolean isQuickMeeting;
}