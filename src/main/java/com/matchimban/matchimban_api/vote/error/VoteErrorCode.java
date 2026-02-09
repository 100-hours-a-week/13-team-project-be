package com.matchimban.matchimban_api.vote.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum VoteErrorCode implements ErrorCode {

    // 403
    FORBIDDEN_NOT_HOST(HttpStatus.FORBIDDEN, "호스트만 가능한 요청입니다."),
    FORBIDDEN_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "모임 참여자가 아닙니다."),
    FORBIDDEN_NOT_ACTIVE_PARTICIPANT(HttpStatus.FORBIDDEN, "활성 참여자가 아닙니다."),

    // 404
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."),
    CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "후보를 찾을 수 없습니다."),
    FINAL_SELECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "최종 선택 결과가 없습니다."),
    NO_RESTAURANTS_FOUND(HttpStatus.NOT_FOUND, "추천 가능한 식당이 없습니다."),

    // 409
    VOTE_CREATE_NOT_READY_HEADCOUNT(HttpStatus.CONFLICT, "아직 투표를 시작할 수 없습니다.(인원 미충족)"),
    VOTE_NOT_OPEN(HttpStatus.CONFLICT, "현재 투표가 진행중이 아닙니다."),
    VOTE_CANDIDATES_NOT_READY(HttpStatus.CONFLICT, "투표 후보가 아직 준비되지 않았습니다."),
    VOTE_NOT_COUNTED_YET(HttpStatus.CONFLICT, "투표 집계가 아직 완료되지 않았습니다."),
    FINAL_ALREADY_SELECTED(HttpStatus.CONFLICT, "이미 최종 선택이 완료되었습니다."),
    REVOTE_NOT_AVAILABLE(HttpStatus.CONFLICT, "재투표를 시작할 수 없는 상태입니다."),
    VOTE_DEADLINE_PASSED(HttpStatus.CONFLICT, "투표 마감 시간이 지나 재투표를 시작할 수 없습니다."),

    // 502
    AI_RECOMMENDATION_FAILED(HttpStatus.BAD_GATEWAY, "추천 시스템 오류가 발생했습니다."),
    AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "추천 응답 형식이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    VoteErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage() { return message; }
    @Override public String getCode() { return name(); }
}
