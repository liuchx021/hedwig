package com.blueship581.hedwig.dto;

import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.enums.VendorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorConnectionDto {
    private Long id;
    private VendorType vendorType;
    private String vendorUserId;
    private Long primarySubjectId;
    private String primarySubjectName;
    private TokenStatus tokenStatus;
    private Instant tokenExpiresAt;
    private Instant lastSyncedAt;
    private Instant sensorExpiresAt;
}
