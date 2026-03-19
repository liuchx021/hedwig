package com.blueship581.hedwig.vendor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorTokenInfo {
    private String userId;
    private String token;
    private Instant expiresAt;
    private boolean valid;
}
