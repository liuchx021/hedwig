package com.blueship581.hedwig.exception;

/** 厂商集成异常（上游 API 调用失败、令牌无效等）。 */
public class VendorException extends BusinessException {

  public VendorException(String message) {
    super(ErrorCode.VENDOR_ERROR, message);
  }

  public VendorException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public VendorException(String message, Throwable cause) {
    super(ErrorCode.VENDOR_ERROR, message, cause);
  }

  public VendorException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
