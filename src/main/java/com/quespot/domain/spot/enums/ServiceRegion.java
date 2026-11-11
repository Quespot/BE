package com.quespot.domain.spot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ServiceRegion {
    SEOUL("11", "서울특별시", "서울", "geodata/seoul-municipalities.geojson", 25),
    INCHEON("28", "인천광역시", "인천", "geodata/incheon-municipalities.geojson", 11),
    GYEONGGI("41", "경기도", "경기", "geodata/gyeonggi-municipalities.geojson", 31);

    private final String regionCode;
    private final String regionName;
    private final String shortName;
    private final String boundaryResourcePath;
    private final int districtCount;

    public static List<String> regionCodes() {
        return Arrays.stream(values()).map(ServiceRegion::getRegionCode).toList();
    }
}
