package com.quespot.domain.notification.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.notification.dto.RecommendableMissionDTO;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// ST_Distance_Sphere 반경 판정과 "완료·진행 중 제외"는 실DB에서만 검증할 수 있다.
@SpringBootTest
@Testcontainers
class RecommendableMissionRepositoryIntegrationTest {

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

    private static final BigDecimal CITY_HALL_LAT = new BigDecimal("37.5665");
    private static final BigDecimal CITY_HALL_LNG = new BigDecimal("126.9780");

    @Autowired private RecommendableMissionRepository recommendableMissionRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private MissionCandidateRepository missionCandidateRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private MissionAttemptRepository missionAttemptRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void applyGeneratedColumn() {
        // MissionAttempt.active_key는 생성 컬럼인데 ddl-auto가 못 만든다(CLAUDE.md "진행 중 미션 중복 방지").
        jdbcTemplate.execute("""
                ALTER TABLE mission_attempts
                  MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
                    CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', mission_id) END
                  ) STORED
                """);
    }

    private Mission mission(String name, String lat, String lng) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("rec" + (System.nanoTime() % 100_000_000_000_000L))
                .name(name)
                .latitude(new BigDecimal(lat)).longitude(new BigDecimal(lng))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1));
        return missionRepository.save(Mission.publish(candidate));
    }

    @Test
    @Transactional
    void picksMissionInsideRadiusOnly() {
        Mission near = mission("덕수궁", "37.5658", "126.9751");      // ~300m
        mission("잠실", "37.5133", "127.1001");                        // ~12km

        Optional<RecommendableMissionDTO> picked =
                recommendableMissionRepository.pickRandomNearby(9501L, CITY_HALL_LAT, CITY_HALL_LNG, 3000);

        assertThat(picked).isPresent();
        assertThat(picked.get().missionId()).isEqualTo(near.getId());
        assertThat(picked.get().spotName()).isEqualTo("덕수궁");
        assertThat(picked.get().rewardPoint()).isEqualTo(near.getRewardPoint());
    }

    @Test
    @Transactional
    void excludesMissionsUserAlreadyCompletedOrInProgress() {
        Long userId = 9502L;
        Mission done = mission("완료됨", "37.5658", "126.9751");
        Mission running = mission("진행중", "37.5660", "126.9760");
        MissionAttempt doneAttempt = MissionAttempt.start(userId, done);
        doneAttempt.complete(CITY_HALL_LAT, CITY_HALL_LNG, done.getRewardPoint());
        missionAttemptRepository.save(doneAttempt);
        missionAttemptRepository.save(MissionAttempt.start(userId, running));

        Optional<RecommendableMissionDTO> picked =
                recommendableMissionRepository.pickRandomNearby(userId, CITY_HALL_LAT, CITY_HALL_LNG, 3000);

        assertThat(picked).isEmpty();
    }

    @Test
    @Transactional
    void returnsEmptyWhenNothingNearby() {
        mission("잠실", "37.5133", "127.1001");

        assertThat(recommendableMissionRepository.pickRandomNearby(9503L, CITY_HALL_LAT, CITY_HALL_LNG, 3000))
                .isEmpty();
    }
}
