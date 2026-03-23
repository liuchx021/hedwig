package com.blueship581.hedwig.util;

import com.blueship581.hedwig.domain.enums.TrendDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlucoseTrendCalculatorTest {

    @Test
    void returnsNoneWhenPreviousReadingMissing() {
        assertEquals(TrendDirection.NONE, GlucoseTrendCalculator.calculate(6.5, null));
    }

    @Test
    void mapsFlatRange() {
        assertEquals(TrendDirection.FLAT, GlucoseTrendCalculator.calculate(6.7, 6.5));
        assertEquals(TrendDirection.FLAT, GlucoseTrendCalculator.calculate(6.3, 6.5));
    }

    @Test
    void mapsFortyFiveRanges() {
        assertEquals(TrendDirection.FORTY_FIVE_UP, GlucoseTrendCalculator.calculate(6.9, 6.5));
        assertEquals(TrendDirection.FORTY_FIVE_DOWN, GlucoseTrendCalculator.calculate(6.1, 6.5));
    }

    @Test
    void mapsSingleRanges() {
        assertEquals(TrendDirection.SINGLE_UP, GlucoseTrendCalculator.calculate(7.2, 6.5));
        assertEquals(TrendDirection.SINGLE_DOWN, GlucoseTrendCalculator.calculate(5.8, 6.5));
    }

    @Test
    void mapsDoubleRanges() {
        assertEquals(TrendDirection.DOUBLE_UP, GlucoseTrendCalculator.calculate(7.4, 6.5));
        assertEquals(TrendDirection.DOUBLE_DOWN, GlucoseTrendCalculator.calculate(5.6, 6.5));
    }
}
