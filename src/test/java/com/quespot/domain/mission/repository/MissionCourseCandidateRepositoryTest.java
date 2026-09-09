package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.projection.CourseCandidateMissionProjection;
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
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MissionCourseCandidateRepositoryTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("quespot_test")
            .withUsername("test")
            .withPassword("test");

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
    private MissionCourseCandidateRepository candidateRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private MissionCandidateRepository missionCandidateRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private MissionAttemptRepository missionAttemptRepository;

    @Autowired
    private DataSource dataSource;

    private static final BigDecimal ANCHOR_LAT = new BigDecimal("37.5665");
    private static final BigDecimal ANCHOR_LNG = new BigDecimal("126.9780");

    // @SpringBootTest는 클래스당 한 번만 스키마를 만든다 — 테스트 메서드끼리 같은
    // DB를 공유하므로, 반경/개수를 검증하는 이 테스트들은 매번 이전 테스트가 남긴
    // 미션이 섞여 들어오지 않도록 직접 비워야 한다(FK 역순으로 삭제).
    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM mission_attempts");
            statement.execute("DELETE FROM missions");
            statement.execute("DELETE FROM mission_candidates");
            statement.execute("DELETE FROM spots");
        }
    }

    private Mission mission(String suffix, MissionTemplate template, BigDecimal lat, BigDecimal lng) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("c" + suffix + (System.nanoTime() % 100_000L))
                .name("후보 스팟 " + suffix)
                .latitude(lat).longitude(lng)
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, template, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    @Test
    void findsOnlyMissionsWithinRadiusMatchingCategoryOrderedByDistance() {
        Mission near = mission("near", MissionTemplate.CULTURE_LOCATION, new BigDecimal("37.5701"), ANCHOR_LNG); // ~400m
        Mission mid = mission("mid", MissionTemplate.NATURE_LOCATION, new BigDecimal("37.5728"), ANCHOR_LNG); // ~700m
        mission("far", MissionTemplate.CULTURE_LOCATION, new BigDecimal("37.5773"), ANCHOR_LNG); // ~1200m, 반경 밖
        mission("wrong-category", MissionTemplate.FOOD_LOCATION, new BigDecimal("37.5670"), ANCHOR_LNG); // 카테고리 불일치

        List<CourseCandidateMissionProjection> result = candidateRepository.findNearestCandidates(
                ANCHOR_LAT, ANCHOR_LNG, 1000,
                List.of("NATURE", "CULTURE"), List.of(-1L), 9999L, 20
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMissionId()).isEqualTo(near.getId());
        assertThat(result.get(1).getMissionId()).isEqualTo(mid.getId());
    }

    @Test
    void excludesMissionsAlreadyAttemptedByUser() {
        Mission attempted = mission("attempted", MissionTemplate.CULTURE_LOCATION, new BigDecimal("37.5670"), ANCHOR_LNG);
        missionAttemptRepository.save(MissionAttempt.start(7001L, attempted));

        List<CourseCandidateMissionProjection> result = candidateRepository.findNearestCandidates(
                ANCHOR_LAT, ANCHOR_LNG, 1000,
                List.of("CULTURE"), List.of(-1L), 7001L, 20
        );

        assertThat(result).noneMatch(row -> row.getMissionId().equals(attempted.getId()));
    }

    @Test
    void excludesGivenExcludeIds() {
        Mission mission = mission("exclude-me", MissionTemplate.CULTURE_LOCATION, new BigDecimal("37.5670"), ANCHOR_LNG);

        List<CourseCandidateMissionProjection> result = candidateRepository.findNearestCandidates(
                ANCHOR_LAT, ANCHOR_LNG, 1000,
                List.of("CULTURE"), List.of(mission.getId()), 9999L, 20
        );

        assertThat(result).noneMatch(row -> row.getMissionId().equals(mission.getId()));
    }

    @Test
    void returnsEmptyWhenNoCandidateWithinRadius() {
        mission("too-far", MissionTemplate.CULTURE_LOCATION, new BigDecimal("37.6000"), ANCHOR_LNG); // ~3.7km

        List<CourseCandidateMissionProjection> result = candidateRepository.findNearestCandidates(
                ANCHOR_LAT, ANCHOR_LNG, 500,
                List.of("CULTURE"), List.of(-1L), 9999L, 20
        );

        assertThat(result).isEmpty();
    }
}
