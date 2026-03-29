package com.blueship581.hedwig.vendor.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiSensingLoginPayload {
  private String phone;
  private String password;
}
