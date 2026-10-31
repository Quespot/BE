package com.quespot.domain.spot.model;

import java.math.BigDecimal;

public record AdministrativeDistrict(
        String regionCode,
        String regionName,
        String districtCode,
        String districtName,
        BigDecimal centerLatitude,
        BigDecimal centerLongitude
) {
}
