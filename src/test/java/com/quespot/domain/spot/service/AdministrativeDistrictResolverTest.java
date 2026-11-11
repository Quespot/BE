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

    @Test
    void exposesDistrictsForIncheonAndGyeonggi() {
        assertThat(resolver.findByRegionCode("28")).hasSize(11);
        assertThat(resolver.findByRegionCode("41")).hasSize(31);
    }

    @Test
    void resolvesGyeonggiCoordinateToCityLevelDistrict() {
        var district = resolver.resolve(
                new BigDecimal("37.263573"),
                new BigDecimal("127.028601"),
                null
        );

        assertThat(district).isPresent();
        assertThat(district.orElseThrow().regionCode()).isEqualTo("41");
        assertThat(district.orElseThrow().districtName()).isEqualTo("수원시");
    }

    @Test
    void resolvesIncheonCoordinate() {
        var district = resolver.resolve(
                new BigDecimal("37.507031"),
                new BigDecimal("126.721804"),
                null
        );

        assertThat(district).isPresent();
        assertThat(district.orElseThrow().regionCode()).isEqualTo("28");
        assertThat(district.orElseThrow().districtName()).isEqualTo("부평구");
    }

    @Test
    void addressFallbackDoesNotMatchSameNamedDistrictInOtherRegion() {
        var district = resolver.resolve(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "인천광역시 중구 공항로"
        );

        assertThat(district).isEmpty();
    }
}
