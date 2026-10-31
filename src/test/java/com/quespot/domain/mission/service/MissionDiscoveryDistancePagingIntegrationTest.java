package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.res.MissionListItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionDiscoveryQueryRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 반경 단계 확장 조회(공간 인덱스)가 기존 전체 거리순 조회와 같은 순서로 모든 미션을 돌려주는지 검증
@SpringBootTest
@Testcontainers
class MissionDiscoveryDistancePagingIntegrationTest {

    private static final long USER_ID = 9301L;
    private static final BigDecimal ORIGIN_LATITUDE = new BigDecimal("37.5665");
    private static final BigDecimal ORIGIN_LONGITUDE = new BigDecimal("126.9780");
    private static final double METERS_PER_DEGREE = 111_195;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("quespot_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("app.jwt.secret", () -> "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz");
        registry.add("app.mail.verification-code-secret", () -> "integration-test-mail-secret");
    }

    @Autowired
    private MissionDiscoveryQueryService missionDiscoveryQueryService;

    @Autowired
    private MissionDiscoveryQueryRepository missionDiscoveryQueryRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private MissionCandidateRepository missionCandidateRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM mission_attempts");
            statement.execute("DELETE FROM missions");
            statement.execute("DELETE FROM mission_candidates");
            statement.execute("DELETE FROM spots");
        }
    }

    @Test
    void createsSpatialIndexOnGeneratedLocationColumn() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            var column = statement.executeQuery("""
                    SELECT generation_expression, srs_id FROM information_schema.columns
                     WHERE table_schema = DATABASE() AND table_name = 'missions' AND column_name = 'snapshot_location'
                    """);
            assertThat(column.next()).isTrue();
            assertThat(column.getString("generation_expression")).contains("snapshot_longitude", "snapshot_latitude");
            assertThat(column.getInt("srs_id")).isZero();

            var index = statement.executeQuery("""
                    SELECT index_type FROM information_schema.statistics
                     WHERE table_schema = DATABASE() AND table_name = 'missions' AND index_name = 'sx_missions_location'
                    """);
            assertThat(index.next()).isTrue();
            assertThat(index.getString("index_type")).isEqualTo("SPATIAL");
        }
    }

    @Test
    void returnsSameOrderAsFullDistanceScanAcrossRadiusSteps() {
        int index = 0;
        double[] distances = {300, 1_200, 4_200, 9_000, 18_000, 26_000, 47_000, 80_000, 150_000, 320_000};
        double[] bearings = {0, 45, 90, 135, 180, 225, 270, 315};
        for (double distance : distances) {
            for (double bearing : bearings) {
                createMission(index++, distance, bearing);
            }
        }
        // 5km 박스 경계 근처 (경도 방향은 위도 보정이 필요해 누락되기 쉬운 위치)
        createMission(index++, 4_990, 90);
        createMission(index++, 4_990, 45);
        createMission(index++, 4_995, 0);
        // 같은 좌표의 미션 (거리 동점 시 id 순서 유지)
        createMission(index++, 7_000, 60);
        createMission(index++, 7_000, 60);

        List<Long> expected = missionDiscoveryQueryRepository.findMissionListByDistance(
                        null, null, ORIGIN_LATITUDE, ORIGIN_LONGITUDE, null, null, 1_000
                ).stream()
                .map(MissionListProjection::getMissionId)
                .toList();

        List<Long> actual = new ArrayList<>();
        String cursor = null;
        MissionListResponseDTO response;
        do {
            response = missionDiscoveryQueryService.getMissions(
                    USER_ID, null, null, ORIGIN_LATITUDE, ORIGIN_LONGITUDE, cursor, 4
            );
            response.missions().stream().map(MissionListItemResponseDTO::missionId).forEach(actual::add);
            cursor = response.nextCursor();
        } while (response.hasNext());

        assertThat(expected).hasSize(index);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private void createMission(int index, double distanceMeters, double bearingDegrees) {
        double bearing = Math.toRadians(bearingDegrees);
        double originLatitude = ORIGIN_LATITUDE.doubleValue();
        double latitude = originLatitude + distanceMeters * Math.cos(bearing) / METERS_PER_DEGREE;
        double longitude = ORIGIN_LONGITUDE.doubleValue()
                + distanceMeters * Math.sin(bearing) / (METERS_PER_DEGREE * Math.cos(Math.toRadians(originLatitude)));

        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API)
                .sourceContentId("dist-" + index)
                .name("거리 테스트 장소 " + index)
                .latitude(BigDecimal.valueOf(latitude).setScale(10, RoundingMode.HALF_UP))
                .longitude(BigDecimal.valueOf(longitude).setScale(10, RoundingMode.HALF_UP))
                .appCategory(AppCategory.HISTORY)
                .categoryMappingVersion(1)
                .showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.HISTORY_LOCATION, 1)
        );
        Mission mission = missionRepository.save(Mission.publish(candidate));
        assertThat(mission.getId()).isNotNull();
    }
}
