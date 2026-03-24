package com.blueship581.hedwig.vendor.model;

import com.blueship581.hedwig.domain.enums.TrendDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorGlucoseData {
  private Double glucoseMmol;
  private Instant readingTime;
  private TrendDirection trendDirection;
}
