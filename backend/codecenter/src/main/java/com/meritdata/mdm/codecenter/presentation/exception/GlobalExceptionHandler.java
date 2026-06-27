package com.meritdata.mdm.codecenter.presentation.exception;

import com.meritdata.mdm.codecenter.common.api.ApiResponse;
import com.meritdata.mdm.codecenter.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理 - 统一错误码 + 人类可读消息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex) {
        log.warn("BizException: code={}, msg={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResponse.fail(msg, "CODECENTER-VALID-5001"));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResponse.fail(msg, "CODECENTER-VALID-5001"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(
                "Data integrity violation: " + extractFriendlyMessage(ex),
                "CODECENTER-DATA-7001"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ex.getMessage(), "CODECENTER-VALID-5002"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("Illegal arg: {}", ex.getMessage());
        return ResponseEntity.ok(ApiResponse.fail(ex.getMessage(), "CODECENTER-VALID-5003"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Internal server error: " + ex.getMessage(),
                        "CODECENTER-SYS-9999"));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private String extractFriendlyMessage(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg == null) return "data integrity violation";
        if (msg.contains("Duplicate entry")) return "duplicate entry";
        if (msg.contains("cannot be null")) return "required field is null";
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
