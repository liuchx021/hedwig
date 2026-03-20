package com.blueship581.hedwig.exception;

import lombok.Getter;

/**
 * 业务异常基类。
 * <p>
 * 所有可预期的业务异常都应继承此类，携带 {@link ErrorCode} 以便
 * {@link GlobalExceptionHandler} 统一映射为标准响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义消息覆盖默认错误码消息（用于需要动态拼接上下文信息的场景）。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
