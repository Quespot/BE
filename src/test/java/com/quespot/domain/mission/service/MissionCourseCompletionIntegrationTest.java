package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
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

// 미션 완료 + 미션 보상 + 코스 완주 + 완주 보너스가 실제로 하나의 트랜잭션인지
// 실DB로 검증한다(#37의 MissionArrivalServiceIntegrationTest 패턴을 코스까지 확장).
@SpringBootTest
@Testcontainers
class MissionCourseCompletionIntegrationTest {

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
    private MissionCourseRepository missionCourseRepository;

    @Autowired
    private CourseMissionRepository courseMissionRepository;

    @Autowired
    private CourseAttemptRepository courseAttemptRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private DataSource dataSource;

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
                .source(SpotSource.TOUR_API).sourceContentId("cc" + suffix + (System.nanoTime() % 100_000_000L))
                .name("통합테스트 스팟 " + suffix)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    private MissionCourse createCourse(Mission... missions) {
        MissionCourse course = missionCourseRepository.save(newCourseEntity());
        int seq = 1;
        for (Mission mission : missions) {
            courseMissionRepository.save(newCourseMissionEntity(course, mission, seq++));
        }
        return course;
    }

    // MissionCourse/CourseMission은 정적 팩토리가 없다(SQL 등록 전제) — 통합
    // 테스트에서만 리플렉션으로 필드를 채워 저장한다.
    private MissionCourse newCourseEntity() {
        try {
            var constructor = MissionCourse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MissionCourse course = constructor.newInstance();
            setField(course, "name", "정동 도보 코스");
            setField(course, "totalRewardPoint", 200);
            setField(course, "bonusPoint", 50);
            setField(course, "missionCount", 2);
            setField(course, "estimatedMinutes", 60);
            setField(course, "status", MissionCourseStatus.ACTIVE);
            return course;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CourseMission newCourseMissionEntity(MissionCourse course, Mission mission, int seq) {
        try {
            var constructor = CourseMission.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CourseMission courseMission = constructor.newInstance();
            setField(courseMission, "course", course);
            setField(courseMission, "mission", mission);
            setField(courseMission, "seq", seq);
            return courseMission;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void completingLastMissionInCourseCompletesCourseAndCreditsBonusInSameTransaction() {
        Mission mission1 = createPublishedMission("a");
        Mission mission2 = createPublishedMission("b");
        MissionCourse course = createCourse(mission1, mission2);
        CourseAttempt courseAttempt = courseAttemptRepository.save(CourseAttempt.start(9101L, course));
        // 첫 번째 미션을 실제 arrive() 흐름으로 먼저 완료시킨다(포인트 지급까지
        // 정상적으로 거쳐야 두 번째 미션 완료 시 코스 완주 총합이 맞다 — complete()를
        // 직접 호출하는 건 포인트 지급 없이 상태만 바꾸는 지름길이라 실제 흐름과
        // 다르다).
        MissionAttempt firstAttempt = missionAttemptRepository.save(
                MissionAttempt.start(9101L, mission1, courseAttempt.getId())
        );
        missionArrivalService.arrive(9101L, firstAttempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));
        MissionAttempt secondAttempt = missionAttemptRepository.save(
                MissionAttempt.start(9101L, mission2, courseAttempt.getId())
        );

        missionArrivalService.arrive(9101L, secondAttempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        CourseAttempt reloaded = courseAttemptRepository.findById(courseAttempt.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CourseAttemptStatus.COMPLETED);
        assertThat(reloaded.getEarnedBonusPoint()).isEqualTo(50);
        assertThat(userPointRepository.findById(9101L).get().getBalance())
                .isEqualTo(mission1.getRewardPoint() + mission2.getRewardPoint() + 50);
    }

    @Test
    void whenBonusCreditFailsCourseAndMissionBothStayUncompletedTogether() {
        Mission mission1 = createPublishedMission("c");
        Mission mission2 = createPublishedMission("d");
        MissionCourse course = createCourse(mission1, mission2);
        CourseAttempt courseAttempt = courseAttemptRepository.save(CourseAttempt.start(9102L, course));
        MissionAttempt firstAttempt = missionAttemptRepository.save(
                MissionAttempt.start(9102L, mission1, courseAttempt.getId())
        );
        missionArrivalService.arrive(9102L, firstAttempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"));
        MissionAttempt secondAttempt = missionAttemptRepository.save(
                MissionAttempt.start(9102L, mission2, courseAttempt.getId())
        );
        // 보너스 지급 시점에 쓰일 정확한 유니크 키로 미리 충돌 행을 심어 credit()을 강제로 실패시킨다.
        pointTransactionRepository.saveAndFlush(
                PointTransaction.earn(9102L, 50, "COURSE_BONUS", "COURSE_ATTEMPT", courseAttempt.getId(), 0)
        );

        assertThatThrownBy(() ->
                missionArrivalService.arrive(9102L, secondAttempt.getId(), new BigDecimal("37.5665"), new BigDecimal("126.9780"))
        ).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        MissionAttempt reloadedSecond = missionAttemptRepository.findById(secondAttempt.getId()).orElseThrow();
        CourseAttempt reloadedCourse = courseAttemptRepository.findById(courseAttempt.getId()).orElseThrow();
        assertThat(reloadedSecond.getStatus()).isEqualTo(MissionAttemptStatus.IN_PROGRESS);
        assertThat(reloadedCourse.getStatus()).isEqualTo(CourseAttemptStatus.IN_PROGRESS);
    }
}
