package com.blueship581.hedwig.exception;

/** 资源不存在异常。 */
public class ResourceNotFoundException extends BusinessException {

  public ResourceNotFoundException(String message) {
    super(ErrorCode.RESOURCE_NOT_FOUND, message);
  }

  public ResourceNotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public ResourceNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}
