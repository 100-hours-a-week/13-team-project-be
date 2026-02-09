package com.matchimban.matchimban_api.global.error.code;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getStatus();
    String getMessage();
    String getCode();
}
