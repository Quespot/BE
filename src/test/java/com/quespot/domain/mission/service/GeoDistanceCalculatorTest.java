package com.quespot.domain.mission.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GeoDistanceCalculatorTest {

    private final GeoDistanceCalculator calculator = new GeoDistanceCalculator();

    @Test
    void samePointIsZeroDistance() {
        long distance = calculator.distanceMeters(
                new BigDecimal("37.5665"), new BigDecimal("126.9780"),
                new BigDecimal("37.5665"), new BigDecimal("126.9780")
        );

        assertThat(distance).isZero();
    }

    @Test
    void oneDegreeLatitudeIsApproximatelyEarthCircumferenceOver360() {
        // 적도 기준 위도 1도 차이는 지구 둘레(2*pi*6371000m)/360 ≈ 111,195m.
        long distance = calculator.distanceMeters(
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ONE, BigDecimal.ZERO
        );

        assertThat(distance).isBetween(111_100L, 111_300L);
    }
}
