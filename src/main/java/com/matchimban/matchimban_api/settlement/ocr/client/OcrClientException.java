package com.matchimban.matchimban_api.settlement.ocr.client;

import lombok.Getter;

@Getter
public class OcrClientException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    public OcrClientException(String code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }
}