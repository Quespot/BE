package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.reward.entity.PointTransaction;
import com.quespot.domain.reward.repository.PointTransactionRepository;
import com.quespot.domain.reward.repository.UserPointRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 완료 처리 + 포인트 지급이 실제로 하나의 트랜잭션으로 묶이는지, 그리고
// active_key UNIQUE가 실제로 진행 중 중복 시작을 막는지 실DB로 검증한다.
@SpringBootTest
@Testcontainers
class MissionArrivalServiceIntegrationTest {

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
    private MissionArrivalService missionArrivalService;

    @Autowired
    private MissionAttemptRepository missionAttemptRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private MissionCandidateRepository missionCandidateRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private DataSource dataSource;

    // Hibernate는 생성 컬럼을 만들지 못해 ddl-auto: create가 active_key를
    // 평범한 nullable VARCHAR(64)로만 만든다. active_key UNIQUE 제약이 실제로
    // 작동하는지 검증하려면 MissionAttempt.java 상단 주석의 ALTER를 스키마
    // 생성 직후 직접 적용해야 한다.
    @BeforeEach
    void applyGeneratedColumn() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE mission_attempts
                    MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
                        CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', mission_id) END
                    ) STORED
                    """);
        } catch (SQLException e) {
            // 이미 생성 컬럼으로 변경된 상태(같은 컨테이너를 여러 테스트가 재사용)면
            // 재적용 시 에러가 날 수 있어 무시한다.
        }
    }

    private Mission createPublishedMission() {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("ia" + (System.nanoTime() % 100_000_000_000_000L))
                .name("통합테스트 스팟")
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    @Test
    void completingAttemptWithinRadiusCommitsBothAttemptAndPointCreditTogether() {
        Mission mission = createPublishedMission();
        MissionAttempt attempt = missionAttemptRepository.save(MissionAttempt.start(9002L, mission));

        missionArrivalService.arrive(9002L, attempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        MissionAttempt reloaded = missionAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MissionAttemptStatus.COMPLETED);
        assertThat(userPointRepository.findById(9002L)).isPresent();
        assertThat(userPointRepository.findById(9002L).get().getBalance()).isEqualTo(mission.getRewardPoint());
    }

    @Test
    void startingSameMissionTwiceWhileInProgressDoesNotCreateSecondRow() {
        Mission mission = createPublishedMission();
        missionAttemptRepository.save(MissionAttempt.start(9003L, mission));

        assertThatThrownBy(() ->
                missionAttemptRepository.saveAndFlush(MissionAttempt.start(9003L, mission))
        ).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void whenPointCreditFailsTheAttemptStaysInProgressInsteadOfBeingCorruptedToCompleted() {
        // point_transactions의 UNIQUE(user_id, type, reference_type, reference_id)와
        // 똑같은 키로 행을 미리 심어둬서, arrive() 안의 credit()이 반드시
        // DataIntegrityViolationException으로 실패하도록 강제한다. arrive()가
        // 완료 처리와 포인트 지급을 정말 하나의 트랜잭션으로 묶고 있다면,
        // 이 실패로 attempt의 COMPLETED 변경도 함께 롤백되어야 한다 — 만약
        // credit()이 REQUIRES_NEW였다면 이 실패는 credit() 쪽 트랜잭션에만
        // 갇히고, attempt는 (이미 커밋된) COMPLETED로 남아있는 모순이 생긴다.
        Mission mission = createPublishedMission();
        MissionAttempt attempt = missionAttemptRepository.save(MissionAttempt.start(9004L, mission));
        pointTransactionRepository.saveAndFlush(
                PointTransaction.earn(9004L, mission.getRewardPoint(), "MISSION_REWARD", "MISSION_ATTEMPT", attempt.getId(), 0)
        );

        assertThatThrownBy(() ->
                missionArrivalService.arrive(9004L, attempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"))
        ).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        MissionAttempt reloaded = missionAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MissionAttemptStatus.IN_PROGRESS);
        assertThat(reloaded.getEarnedPoint()).isNull();
    }
}
