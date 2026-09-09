package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.mission.repository.MissionRepository;
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

// 순차 잠금이 courseAttemptId 없이도 실제로 막히는지 실DB로 검증한다(#39
// 재설계의 핵심 요구사항 — 미션 목록에서 우회 진입을 막는 지점이 진짜 동작하는지
// 목업이 아니라 MissionAttemptService.start()의 실제 흐름으로 확인한다).
@SpringBootTest
@Testcontainers
class MissionCourseSequentialLockIntegrationTest {

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

    @Autowired private MissionAttemptService missionAttemptService;
    @Autowired private MissionArrivalService missionArrivalService;
    @Autowired private MissionAttemptRepository missionAttemptRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private MissionCandidateRepository missionCandidateRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private MissionCourseRepository missionCourseRepository;
    @Autowired private CourseMissionRepository courseMissionRepository;
    @Autowired private CourseAttemptRepository courseAttemptRepository;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void applyGeneratedColumns() throws SQLException {
        applyAlter("""
                ALTER TABLE mission_attempts
                MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
                    CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', mission_id) END
                ) STORED
                """);
        applyAlter("""
                ALTER TABLE course_attempts
                MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
                    CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', course_id) END
                ) STORED
                """);
    }

    private void applyAlter(String sql) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            // 컨테이너 재사용으로 이미 생성 컬럼인 경우 무시.
        }
    }

    private Mission createPublishedMission(String suffix) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("lk" + suffix + (System.nanoTime() % 100_000_000L))
                .name("잠금테스트 스팟 " + suffix)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    @Test
    void startingSecondMissionWithoutCourseAttemptIdIsBlockedUntilFirstCompleted() {
        Mission mission1 = createPublishedMission("first");
        Mission mission2 = createPublishedMission("second");
        MissionCourse course = missionCourseRepository.save(MissionCourse.generate(
                mission1, 8001L, 2, "잠금 테스트 코스", null, null, null, 200, 60, 60
        ));
        courseMissionRepository.save(CourseMission.of(course, mission1, 1));
        courseMissionRepository.save(CourseMission.of(course, mission2, 2));
        courseAttemptRepository.save(CourseAttempt.start(8001L, course));

        // courseAttemptId 없이 목록 화면에서 바로 2번 미션을 시작하려는 시도 — 막혀야 한다.
        assertThatThrownBy(() -> missionAttemptService.start(8001L, mission2.getId()))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_LOCKED);

        // 1번을 실제로 완료시킨다.
        MissionAttempt firstAttempt = missionAttemptRepository.save(MissionAttempt.start(8001L, mission1));
        missionArrivalService.arrive(8001L, firstAttempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        // 이제 courseAttemptId 없이도 2번 미션이 정상적으로 시작된다.
        MissionAttempt secondAttempt = missionAttemptService.start(8001L, mission2.getId());
        assertThat(secondAttempt.getStatus().name()).isEqualTo("IN_PROGRESS");
    }
}
