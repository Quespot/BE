package com.quespot.domain.mission.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// MissionQueryService(#35)에 동일한 하버사인 공식이 이미 private 메서드로
// 있지만, 건우 소유 클래스의 private 메서드라 재사용할 권한이 없다. 이거
// 하나 때문에 그 클래스를 리팩토링하는 건 "건우 코드 건드리지 않는다"는
// 범위를 넘어서므로, 15줄짜리 공식을 의도적으로 중복 구현한다.
//
// 하버사인을 쓰는 이유: 500m 반경 판정에 필요한 정확도는 미터 단위면
// 충분하다(반경 자체가 GPS 오차를 흡수하려고 500m로 잡은 값). 이 거리대에서
// 지구를 구로 근사한 오차는 무시할 수준(<1m)이라 Vincenty 같은 정밀 타원체
// 공식은 과설계다. 순수 Java 계산이라 외부 좌표계 라이브러리나 MySQL 공간
// 함수도 필요 없다 — "반경 내 검색"이 아니라 "시도 하나의 두 점 사이 거리"라서.
@Component
public class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public long distanceMeters(
            BigDecimal sourceLatitude,
            BigDecimal sourceLongitude,
            BigDecimal targetLatitude,
            BigDecimal targetLongitude
    ) {
        double sourceLatRadians = Math.toRadians(sourceLatitude.doubleValue());
        double targetLatRadians = Math.toRadians(targetLatitude.doubleValue());
        double latitudeDelta = targetLatRadians - sourceLatRadians;
        double longitudeDelta = Math.toRadians(
                targetLongitude.subtract(sourceLongitude).doubleValue()
        );
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(sourceLatRadians)
                * Math.cos(targetLatRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double centralAngle = 2 * Math.asin(Math.min(1, Math.sqrt(haversine)));
        return Math.round(EARTH_RADIUS_METERS * centralAngle);
    }
}
