package com.matchimban.matchimban_api.meeting.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MeetingErrorCode implements ErrorCode {

    // 400
    INVALID_MEETING_TIME(HttpStatus.BAD_REQUEST, "모임/투표 시간 설정이 올바르지 않습니다."),

    // 403
    NOT_ACTIVE_PARTICIPANT(HttpStatus.FORBIDDEN, "모임 참여자만 접근할 수 있습니다."),
    ONLY_HOST_ALLOWED(HttpStatus.FORBIDDEN, "호스트만 수행할 수 있습니다."),

    // 404
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "모임을 찾을 수 없습니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),

    // 409
    MEETING_FULL(HttpStatus.CONFLICT, "모임 정원이 가득 찼습니다."),
    HOST_CANNOT_LEAVE(HttpStatus.CONFLICT, "호스트는 모임을 탈퇴할 수 없습니다."),
    VOTE_IN_PROGRESS(HttpStatus.CONFLICT, "투표 진행 중에는 모임을 탈퇴할 수 없습니다."),
    MEETING_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT, "투표가 생성된 이후에는 모임을 수정할 수 없습니다."),
    INVITE_CODE_CONFLICT(HttpStatus.CONFLICT, "초대 코드 생성에 실패했습니다.");


    private final HttpStatus status;
    private final String message;

    MeetingErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage() { return message; }
    @Override public String getCode() { return this.name(); } // code는 enum name()
}
