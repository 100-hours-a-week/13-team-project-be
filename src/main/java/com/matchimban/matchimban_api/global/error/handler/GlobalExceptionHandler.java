package com.matchimban.matchimban_api.global.error.handler;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.global.error.api.ErrorResponse;
import com.matchimban.matchimban_api.global.error.api.FieldErrorData;
import com.matchimban.matchimban_api.global.error.code.CommonErrorCode;
import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 도메인/비즈니스 예외
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorCode ec = ex.getErrorCode();
        ErrorResponse body = new ErrorResponse(ec.getCode(), ec.getMessage(), ex.getErrors(), ex.getData());
        return ResponseEntity.status(ec.getStatus()).body(body);
    }

    // @Valid body 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldErrorData> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorData(fe.getField(), safeReason(fe.getDefaultMessage())))
                .toList();

        ErrorCode ec = CommonErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.getStatus())
                .body(new ErrorResponse(ec.getCode(), ec.getMessage(), errors, null));
    }

    // @RequestParam/@PathVariable 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorData> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();

        ErrorCode ec = CommonErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.getStatus())
                .body(new ErrorResponse(ec.getCode(), ec.getMessage(), errors, null));
    }

    // JSON 파싱 실패
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorCode ec = CommonErrorCode.INVALID_JSON;
        return ResponseEntity.status(ec.getStatus())
                .body(new ErrorResponse(ec.getCode(), ec.getMessage(), null, null));
    }

    // 필수 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        ErrorCode ec = CommonErrorCode.PARAMETER_MISSING;
        List<FieldErrorData> errors = List.of(new FieldErrorData(ex.getParameterName(), "필수값입니다."));
        return ResponseEntity.status(ec.getStatus())
                .body(new ErrorResponse(ec.getCode(), ec.getMessage(), errors, null));
    }

    // 타입 미스매치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorCode ec = CommonErrorCode.TYPE_MISMATCH;
        List<FieldErrorData> errors = List.of(new FieldErrorData(ex.getName(), "형식이 올바르지 않습니다."));
        return ResponseEntity.status(ec.getStatus())
                .body(new ErrorResponse(ec.getCode(), ec.getMessage(), errors, null));
    }

    // 예상 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        LOG.error("Unhandled exception", ex);
        ErrorCode ec = CommonErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(new ErrorResponse(ec.getCode(), ec.getMessage(), null, null));
    }

    private FieldErrorData toFieldError(ConstraintViolation<?> v) {
        String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "unknown";
        String field = extractLastPathToken(path);
        String reason = safeReason(v.getMessage());
        return new FieldErrorData(field, reason);
    }

    private String extractLastPathToken(String path) {
        int idx = path.lastIndexOf('.');
        return (idx >= 0 && idx < path.length() - 1) ? path.substring(idx + 1) : path;
    }

    private String safeReason(String reason) {
        return (reason == null || reason.isBlank()) ? "유효하지 않습니다." : reason;
    }
}
