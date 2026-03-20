package com.blueship581.hedwig.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;

/**
 * 统一 API 响应体。
 * <p>
 * 成功时 {@code code = 0, message = "操作成功", data = 业务数据}。
 * 失败时 {@code code > 0, message = 友好提示, data = null}。
 *
 * @param <T> 业务数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;
    private String timestamp;

    private ApiResult() {
        this.timestamp = Instant.now().toString();
    }

    // ==================== 成功 ====================

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = ErrorCode.SUCCESS.getCode();
        result.message = ErrorCode.SUCCESS.getMessage();
        result.data = data;
        return result;
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.code = ErrorCode.SUCCESS.getCode();
        result.message = message;
        result.data = data;
        return result;
    }

    // ==================== 失败 ====================

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        ApiResult<T> result = new ApiResult<>();
        result.code = errorCode.getCode();
        result.message = errorCode.getMessage();
        return result;
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.code = errorCode.getCode();
        result.message = message;
        return result;
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.code = code;
        result.message = message;
        return result;
    }
}
