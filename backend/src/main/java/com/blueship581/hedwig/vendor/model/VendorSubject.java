package com.blueship581.hedwig.vendor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorSubject {
    // The subject's user ID on the vendor platform
    private String subjectId;
    // The subject's device ID (may be null for some vendors)
    private String deviceId;
    private String displayName;
    // Latest glucose value in mmol/L (if available from list endpoint)
    private Double latestGlucoseMmol;
    // Sensor remaining time in seconds (from Ottai restDeviceTime)
    private Long sensorRestSeconds;
}
