package com.blueship581.hedwig.vendor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorLoginRequest {
  // For token-based (Ottai / SiSensing): provide accessToken directly
  private String accessToken;
  // For username/password login (future extension)
  private String username;
  private String password;
}
