package com.blueship581.hedwig.exception;

/**
 * 认证相关异常（登录、注册、令牌校验等）。
 */
public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
