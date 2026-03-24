package com.blueship581.hedwig.util;

import com.blueship581.hedwig.domain.enums.TrendDirection;

import java.time.Instant;

public final class GlucoseTrendCalculator {

  /**
   * Expected interval between two consecutive CGM readings in seconds. Most CGM sensors produce a
   * reading every 5 minutes (300s).
   */
  private static final double EXPECTED_INTERVAL_SEC = 300.0;

  /**
   * Maximum interval (in seconds) beyond which trend calculation is not meaningful. If two readings
   * are more than 30 minutes apart, the rate-of-change is unreliable.
   */
  private static final long MAX_INTERVAL_SEC = 1800;

  private GlucoseTrendCalculator() {}

  /**
   * Calculate trend direction from two consecutive readings, normalizing by time interval. Falls
   * back to simple delta when timestamps are unavailable.
   *
   * @param currentMmol current glucose value in mmol/L
   * @param previousMmol previous glucose value in mmol/L
   * @param currentTime timestamp of current reading (nullable)
   * @param previousTime timestamp of previous reading (nullable)
   */
  public static TrendDirection calculate(
      Double currentMmol, Double previousMmol, Instant currentTime, Instant previousTime) {
    if (currentMmol == null || previousMmol == null) {
      return TrendDirection.NONE;
    }

    double rawDelta = currentMmol - previousMmol;

    // Normalize delta to a per-5-minute rate if timestamps are available
    double normalizedDelta;
    if (currentTime != null && previousTime != null) {
      long intervalSec = Math.abs(currentTime.getEpochSecond() - previousTime.getEpochSecond());
      if (intervalSec <= 0) {
        return TrendDirection.FLAT;
      }
      if (intervalSec > MAX_INTERVAL_SEC) {
        // Gap too large for meaningful trend — report NONE
        return TrendDirection.NONE;
      }
      // Scale delta to what it would be over one standard 5-minute interval
      normalizedDelta = rawDelta * (EXPECTED_INTERVAL_SEC / intervalSec);
    } else {
      // No timestamps — assume standard 5-minute interval (backward compatible)
      normalizedDelta = rawDelta;
    }

    return classifyDelta(normalizedDelta);
  }

  /**
   * Backward-compatible overload: calculate without timestamps. Assumes the two readings are
   * exactly one standard interval apart.
   */
  public static TrendDirection calculate(Double currentMmol, Double previousMmol) {
    return calculate(currentMmol, previousMmol, null, null);
  }

  private static TrendDirection classifyDelta(double delta) {
    double absDelta = Math.abs(delta);

    if (absDelta < 0.3) {
      return TrendDirection.FLAT;
    }

    if (delta > 0) {
      if (absDelta >= 0.9) {
        return TrendDirection.DOUBLE_UP;
      }
      if (absDelta >= 0.6) {
        return TrendDirection.SINGLE_UP;
      }
      return TrendDirection.FORTY_FIVE_UP;
    }

    if (absDelta >= 0.9) {
      return TrendDirection.DOUBLE_DOWN;
    }
    if (absDelta >= 0.6) {
      return TrendDirection.SINGLE_DOWN;
    }
    return TrendDirection.FORTY_FIVE_DOWN;
  }
}
