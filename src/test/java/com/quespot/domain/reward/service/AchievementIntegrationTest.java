package com.quespot.domain.reward.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.service.MissionArrivalService;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.enums.ActivityType;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.RewardActivityRepository;
import com.quespot.domain.reward.repository.StampRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import com.quespot.domain.reward.repository.UserStampRepository;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

// 도착 인증 한 번으로 첫 미션 배지 + 서울 스탬프 + 활동 2건이 같은 트랜잭션에서
// 생기는지, 두 번째 완료에서 중복 획득이 없는지 실DB로 검증한다.
// 시더(RewardMasterDataSeeder)가 부팅 시 배지 5개·스탬프 8개를 넣어둔다.
@SpringBootTest
@Testcontainers
class AchievementIntegrationTest {

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

    @Autowired private MissionArrivalService missionArrivalService;
    @Autowired private MissionAttemptRepository missionAttemptRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private MissionCandidateRepository missionCandidateRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private BadgeRepository badgeRepository;
    @Autowired private StampRepository stampRepository;
    @Autowired private UserBadgeRepository userBadgeRepository;
    @Autowired private UserStampRepository userStampRepository;
    @Autowired private RewardActivityRepository rewardActivityRepository;
    @Autowired private DataSource dataSource;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void applyGeneratedColumn() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE mission_attempts
                    MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
                        CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', mission_id) END
                    ) STORED
                    """);
        } catch (SQLException e) {
            // 이미 적용된 컨테이너 재사용 시 무시
        }
    }

    private Mission publishedSeoulMission(String signguCd) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("ach" + (System.nanoTime() % 100_000_000_000_000L))
                .name("통합테스트 스팟")
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .ldongRegnCd("11").ldongSignguCd(signguCd)
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    @Test
    void seederLeavesFiveActiveBadgesAndEightStamps() throws Exception {
        assertThat(badgeRepository.countByIsActiveTrue()).isEqualTo(5);
        assertThat(stampRepository.count()).isEqualTo(8);
        // MySQL JSON 컬럼은 저장값을 정규화해(키 순서·공백) 돌려주므로 문자열이 아니라
        // 의미로 비교한다 — 시더의 needsUpdate가 같은 기준을 쓴다.
        String stored = badgeRepository.findByCode("EXPLORER").orElseThrow().getConditionJson();
        assertThat(objectMapper.readTree(stored)).isEqualTo(objectMapper.readTree(
                "{\"metric\":\"MISSION_COMPLETED_DISTINCT_DISTRICT\",\"scope\":{\"regionCode\":\"11\"},\"threshold\":5}"
        ));
    }

    @Test
    void firstArrivalAwardsFirstMissionBadgeAndSeoulStampWithActivities() {
        Long userId = 9201L;
        MissionAttempt attempt = missionAttemptRepository.save(MissionAttempt.start(userId, publishedSeoulMission("110")));

        missionArrivalService.arrive(userId, attempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        Long firstMissionBadgeId = badgeRepository.findByCode("FIRST_MISSION").orElseThrow().getId();
        Long seoulStampId = stampRepository.findByRegionCode("11").orElseThrow().getId();
        assertThat(userBadgeRepository.findBadgeIdsByUserId(userId)).containsExactly(firstMissionBadgeId);
        assertThat(userStampRepository.existsByUserIdAndStamp_Id(userId, seoulStampId)).isTrue();
        assertThat(rewardActivityRepository.findAll())
                .filteredOn(a -> a.getUserId().equals(userId))
                .extracting(RewardActivity::getActivityType)
                .containsExactlyInAnyOrder(ActivityType.POINT_EARNED, ActivityType.BADGE_ACQUIRED, ActivityType.STAMP_ACQUIRED);
    }

    @Test
    void secondArrivalDoesNotDuplicateBadgeOrStamp() {
        Long userId = 9202L;
        MissionAttempt first = missionAttemptRepository.save(MissionAttempt.start(userId, publishedSeoulMission("110")));
        missionArrivalService.arrive(userId, first.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));
        MissionAttempt second = missionAttemptRepository.save(MissionAttempt.start(userId, publishedSeoulMission("140")));

        missionArrivalService.arrive(userId, second.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        assertThat(userBadgeRepository.countByUserId(userId)).isEqualTo(1);
        assertThat(userStampRepository.countByUserId(userId)).isEqualTo(1);
    }
}
