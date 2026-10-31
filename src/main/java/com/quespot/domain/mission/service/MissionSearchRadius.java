package com.quespot.domain.mission.service;

import java.math.BigDecimal;

// 거리순 미션 조회의 탐색 반경 단계 (5km → 20km → 50km → 전국)
// 가까운 반경부터 bounding box로 후보를 줄여 조회하고, 결과가 부족할 때만 다음 반경으로 넓힌다.
public final class MissionSearchRadius {

    private static final int[] RADIUS_METERS = {5_000, 20_000, 50_000};
    // ST_Distance_Sphere 기본 지구 반지름
    private static final double EARTH_RADIUS_METERS = 6_370_986;
    private static final double METERS_PER_DEGREE = Math.PI * EARTH_RADIUS_METERS / 180;
    // 위경도 환산 오차로 경계 미션이 빠지지 않도록 박스를 1% 넓힌다 (실제 반경은 거리 조건으로 다시 거른다)
    private static final double BOX_MARGIN = 1.01;
    private static final double MIN_COS_LATITUDE = 0.01;

    private MissionSearchRadius() {
    }

    public static int unlimitedStep() {
        return RADIUS_METERS.length;
    }

    public static boolean isUnlimited(int step) {
        return step >= RADIUS_METERS.length;
    }

    public static boolean isValidStep(int step) {
        return step >= 0 && step <= RADIUS_METERS.length;
    }

    public static int radiusMeters(int step) {
        return RADIUS_METERS[step];
    }

    public static BoundingBox boundingBox(BigDecimal latitude, BigDecimal longitude, int radiusMeters) {
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        double latDelta = radiusMeters * BOX_MARGIN / METERS_PER_DEGREE;
        double cosLatitude = Math.max(Math.cos(Math.toRadians(lat)), MIN_COS_LATITUDE);
        double lngDelta = latDelta / cosLatitude;
        return new BoundingBox(lat - latDelta, lat + latDelta, lng - lngDelta, lng + lngDelta);
    }

    public record BoundingBox(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
    }
}
