package com.blueship581.hedwig.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return error(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(VendorException.class)
    public ResponseEntity<Map<String, Object>> handleVendorException(VendorException e) {
        log.error("Vendor error: {}", e.getMessage(), e);
        return error(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "系统出现异常，请稍后重试");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", localizeStatus(status),
                "message", message
        ));
    }

    private String localizeStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "请求参数错误";
            case UNAUTHORIZED -> "未登录";
            case FORBIDDEN -> "禁止访问";
            case NOT_FOUND -> "资源不存在";
            case BAD_GATEWAY -> "上游服务异常";
            case INTERNAL_SERVER_ERROR -> "服务器内部错误";
            default -> status.getReasonPhrase();
        };
    }
}
