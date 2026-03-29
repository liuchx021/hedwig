package com.blueship581.hedwig.vendor.client.dto;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class SiSensingApiResponse {
  private Boolean success;
  private Object data;
  private String msg;
  private String message;
  private String error;

  public boolean isFailed() {
    return success != null && !success;
  }

  public JSONObject getDataAsObject() {
    return data instanceof JSONObject ? (JSONObject) data : null;
  }

  @JSONField(serialize = false)
  public String getErrorDetail() {
    if (msg != null && !msg.isBlank()) return msg;
    if (message != null && !message.isBlank()) return message;
    if (error != null && !error.isBlank()) return error;
    return null;
  }
}
