package com.matchimban.matchimban_api.global.error.api;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import lombok.Getter;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorData> errors;
    private final Object data;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public ApiException(ErrorCode errorCode, Object data) {
        this(errorCode, null, data);
    }

    public ApiException(ErrorCode errorCode, List<FieldErrorData> errors, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.errors = errors;
        this.data = data;
    }
}
