package com.blueship581.hedwig.util;

import com.blueship581.hedwig.domain.enums.TrendDirection;

public final class GlucoseTrendCalculator {

    private GlucoseTrendCalculator() {
    }

    public static TrendDirection calculate(Double currentMmol, Double previousMmol) {
        if (currentMmol == null || previousMmol == null) {
            return TrendDirection.NONE;
        }

        double delta = currentMmol - previousMmol;
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
