package com.blueship581.hedwig.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常（统一基类） ====================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException e) {
        ErrorCode ec = e.getErrorCode();
        // 厂商异常打印堆栈便于排查上游问题
        if (e instanceof VendorException) {
            log.error("Vendor error [{}]: {}", ec.getCode(), e.getMessage(), e);
        } else {
            log.warn("Business error [{}]: {}", ec.getCode(), e.getMessage());
        }
        return ResponseEntity.status(ec.getHttpStatus())
                .body(ApiResult.fail(ec, e.getMessage()));
    }

    // ==================== Spring Security 异常 ====================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResult<Void>> handleBadCredentials(BadCredentialsException e) {
        return buildResponse(ErrorCode.BAD_CREDENTIALS);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException e) {
        return buildResponse(ErrorCode.ACCESS_DENIED);
    }

    // ==================== 参数校验 & 请求格式异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(ErrorCode.PARAM_INVALID, detail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return buildResponse(ErrorCode.PARAM_MISSING, "缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return buildResponse(ErrorCode.PARAM_INVALID, "参数类型不匹配：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return buildResponse(ErrorCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 格式");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    // ==================== HTTP 方法 & 媒体类型 ====================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return buildResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return buildResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNoResource(NoResourceFoundException e) {
        return buildResponse(ErrorCode.RESOURCE_NOT_FOUND, "请求的路径不存在：" + e.getResourcePath());
    }

    // ==================== 兜底 ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return buildResponse(ErrorCode.INTERNAL_ERROR);
    }

    // ==================== 工具方法 ====================

    private ResponseEntity<ApiResult<Void>> buildResponse(ErrorCode ec) {
        return ResponseEntity.status(ec.getHttpStatus())
                .body(ApiResult.fail(ec));
    }

    private ResponseEntity<ApiResult<Void>> buildResponse(ErrorCode ec, String message) {
        return ResponseEntity.status(ec.getHttpStatus())
                .body(ApiResult.fail(ec, message));
    }
}
