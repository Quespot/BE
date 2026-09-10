package com.quespot.domain.spot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.spot.model.AdministrativeDistrict;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AdministrativeDistrictResolver {

    private static final List<BoundarySource> BOUNDARY_SOURCES = List.of(
            new BoundarySource(
                    "geodata/seoul-municipalities.geojson",
                    "11",
                    "서울특별시",
                    25
            )
    );

    private final ObjectMapper objectMapper;
    private final Map<String, DistrictBoundary> boundaries = new LinkedHashMap<>();

    public AdministrativeDistrictResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadBoundaries() {
        for (BoundarySource source : BOUNDARY_SOURCES) {
            loadBoundarySource(source);
        }
    }

    public Optional<AdministrativeDistrict> resolve(
            BigDecimal latitude,
            BigDecimal longitude,
            String address
    ) {
        Optional<AdministrativeDistrict> coordinateResult = resolveByCoordinate(latitude, longitude);
        Optional<AdministrativeDistrict> addressResult = resolveByAddress(address);

        if (coordinateResult.isPresent()
                && addressResult.isPresent()
                && !coordinateResult.get().districtCode().equals(addressResult.get().districtCode())) {
            log.warn(
                    "행정구역 좌표/주소 판정 불일치. coordinateDistrict={}, addressDistrict={}, address={}",
                    coordinateResult.get().districtCode(),
                    addressResult.get().districtCode(),
                    address
            );
        }
        return coordinateResult.or(() -> addressResult);
    }

    public Optional<AdministrativeDistrict> findByCode(String districtCode) {
        DistrictBoundary boundary = boundaries.get(districtCode);
        return boundary == null ? Optional.empty() : Optional.of(boundary.district());
    }

    public List<AdministrativeDistrict> findByRegionCode(String regionCode) {
        return boundaries.values().stream()
                .map(DistrictBoundary::district)
                .filter(district -> district.regionCode().equals(regionCode))
                .toList();
    }

    public List<AdministrativeDistrict> findAll() {
        return boundaries.values().stream().map(DistrictBoundary::district).toList();
    }

    private Optional<AdministrativeDistrict> resolveByCoordinate(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }
        double x = longitude.doubleValue();
        double y = latitude.doubleValue();
        return boundaries.values().stream()
                .filter(boundary -> boundary.contains(x, y))
                .map(DistrictBoundary::district)
                .findFirst();
    }

    private Optional<AdministrativeDistrict> resolveByAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        return boundaries.values().stream()
                .map(DistrictBoundary::district)
                .filter(district -> address.contains(district.districtName()))
                .findFirst();
    }

    private void loadBoundarySource(BoundarySource source) {
        int beforeCount = boundaries.size();
        try (var inputStream = new ClassPathResource(source.resourcePath()).getInputStream()) {
            JsonNode features = objectMapper.readTree(inputStream).path("features");
            for (JsonNode feature : features) {
                DistrictBoundary boundary = toBoundary(feature, source);
                boundaries.put(boundary.district().districtCode(), boundary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("행정구역 경계 데이터를 읽을 수 없습니다.", exception);
        }
        if (boundaries.size() - beforeCount != source.expectedDistrictCount()) {
            throw new IllegalStateException(source.regionName() + " 행정구역 경계 개수가 올바르지 않습니다.");
        }
    }

    private DistrictBoundary toBoundary(JsonNode feature, BoundarySource source) {
        JsonNode properties = feature.path("properties");
        String districtCode = properties.path("SIG_CD").asText();
        String districtName = properties.path("SIG_KOR_NM").asText();
        JsonNode geometry = feature.path("geometry");
        List<PolygonBoundary> polygons = parsePolygons(geometry);
        Coordinate center = calculateCenter(polygons);

        AdministrativeDistrict district = new AdministrativeDistrict(
                source.regionCode(),
                source.regionName(),
                districtCode,
                districtName,
                BigDecimal.valueOf(center.latitude()),
                BigDecimal.valueOf(center.longitude())
        );
        return new DistrictBoundary(district, polygons);
    }

    private List<PolygonBoundary> parsePolygons(JsonNode geometry) {
        String type = geometry.path("type").asText();
        JsonNode coordinates = geometry.path("coordinates");
        List<PolygonBoundary> polygons = new ArrayList<>();

        if ("Polygon".equals(type)) {
            polygons.add(parsePolygon(coordinates));
        } else if ("MultiPolygon".equals(type)) {
            for (JsonNode polygon : coordinates) {
                polygons.add(parsePolygon(polygon));
            }
        } else {
            throw new IllegalStateException("지원하지 않는 행정구역 geometry입니다: " + type);
        }
        return List.copyOf(polygons);
    }

    private PolygonBoundary parsePolygon(JsonNode polygonNode) {
        List<List<Coordinate>> rings = new ArrayList<>();
        for (JsonNode ringNode : polygonNode) {
            List<Coordinate> ring = new ArrayList<>();
            for (JsonNode coordinate : ringNode) {
                ring.add(new Coordinate(coordinate.get(0).asDouble(), coordinate.get(1).asDouble()));
            }
            rings.add(List.copyOf(ring));
        }
        if (rings.isEmpty()) {
            throw new IllegalStateException("행정구역 polygon에 좌표가 없습니다.");
        }
        return new PolygonBoundary(rings.get(0), List.copyOf(rings.subList(1, rings.size())));
    }

    private Coordinate calculateCenter(List<PolygonBoundary> polygons) {
        PolygonBoundary largest = polygons.stream()
                .max((left, right) -> Double.compare(left.absoluteArea(), right.absoluteArea()))
                .orElseThrow();
        return largest.centroid();
    }

    private record DistrictBoundary(
            AdministrativeDistrict district,
            List<PolygonBoundary> polygons
    ) {
        boolean contains(double longitude, double latitude) {
            return polygons.stream().anyMatch(polygon -> polygon.contains(longitude, latitude));
        }
    }

    private record PolygonBoundary(
            List<Coordinate> outerRing,
            List<List<Coordinate>> holes
    ) {
        boolean contains(double longitude, double latitude) {
            if (!isInsideRing(outerRing, longitude, latitude)) {
                return false;
            }
            return holes.stream().noneMatch(hole -> isInsideRing(hole, longitude, latitude));
        }

        double absoluteArea() {
            return Math.abs(signedArea(outerRing));
        }

        Coordinate centroid() {
            double areaFactor = 0;
            double longitudeSum = 0;
            double latitudeSum = 0;
            for (int i = 0; i < outerRing.size() - 1; i++) {
                Coordinate current = outerRing.get(i);
                Coordinate next = outerRing.get(i + 1);
                double cross = current.longitude() * next.latitude()
                        - next.longitude() * current.latitude();
                areaFactor += cross;
                longitudeSum += (current.longitude() + next.longitude()) * cross;
                latitudeSum += (current.latitude() + next.latitude()) * cross;
            }
            if (Math.abs(areaFactor) < 1e-12) {
                return outerRing.get(0);
            }
            return new Coordinate(
                    longitudeSum / (3 * areaFactor),
                    latitudeSum / (3 * areaFactor)
            );
        }

        private static double signedArea(List<Coordinate> ring) {
            double sum = 0;
            for (int i = 0; i < ring.size() - 1; i++) {
                Coordinate current = ring.get(i);
                Coordinate next = ring.get(i + 1);
                sum += current.longitude() * next.latitude() - next.longitude() * current.latitude();
            }
            return sum / 2;
        }

        private static boolean isInsideRing(List<Coordinate> ring, double x, double y) {
            boolean inside = false;
            for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
                Coordinate current = ring.get(i);
                Coordinate previous = ring.get(j);
                boolean crosses = (current.latitude() > y) != (previous.latitude() > y)
                        && x < (previous.longitude() - current.longitude())
                        * (y - current.latitude())
                        / (previous.latitude() - current.latitude())
                        + current.longitude();
                if (crosses) {
                    inside = !inside;
                }
            }
            return inside;
        }
    }

    private record Coordinate(double longitude, double latitude) {
    }

    private record BoundarySource(
            String resourcePath,
            String regionCode,
            String regionName,
            int expectedDistrictCount
    ) {
    }
}
