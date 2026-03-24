package com.blueship581.hedwig.dto;

import com.blueship581.hedwig.domain.enums.VendorType;
import lombok.Data;

@Data
public class ConnectByTokenRequest {
  private VendorType vendorType;
  private String accessToken;
}
