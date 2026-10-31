package com.quespot.domain.spot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrativeDistrictResolverTest {

    private AdministrativeDistrictResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AdministrativeDistrictResolver(new ObjectMapper());
        resolver.loadBoundaries();
    }

    @Test
    void resolvesDistrictByCoordinateFirst() {
        var district = resolver.resolve(
                new BigDecimal("37.579617"),
                new BigDecimal("126.977041"),
                "서울특별시 강남구"
        );

        assertThat(district).isPresent();
        assertThat(district.orElseThrow().districtCode()).isEqualTo("11110");
        assertThat(district.orElseThrow().districtName()).isEqualTo("종로구");
    }

    @Test
    void fallsBackToAddressWhenCoordinateIsOutsideLoadedBoundaries() {
        var district = resolver.resolve(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "서울특별시 강남구 테헤란로"
        );

        assertThat(district).isPresent();
        assertThat(district.orElseThrow().districtCode()).isEqualTo("11680");
    }

    @Test
    void exposesAllTwentyFiveDistrictsForSeoul() {
        assertThat(resolver.findByRegionCode("11")).hasSize(25);
    }
}
