package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.projection.CourseMissionLockRowProjection;
import com.quespot.domain.mission.repository.projection.ExistingCoursePairProjection;
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
class CourseMissionRepositoryTest {

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

    @Autowired private CourseMissionRepository courseMissionRepository;
    @Autowired private MissionCourseRepository missionCourseRepository;
    @Autowired private CourseAttemptRepository courseAttemptRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private MissionCandidateRepository missionCandidateRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private DataSource dataSource;

    // 이 클래스의 여러 @Test가 같은 컨테이너/스키마를 공유한다(@SpringBootTest는
    // 클래스당 한 번만 초기화됨) — 반경/개수 검증이 아니라 특정 행 검증 위주라
    // 실제로는 없어도 되지만, 다른 리포지토리 테스트와 동일한 안전장치를 둔다.
    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM course_attempts");
            statement.execute("DELETE FROM course_missions");
            statement.execute("DELETE FROM mission_courses");
            statement.execute("DELETE FROM mission_attempts");
            statement.execute("DELETE FROM missions");
            statement.execute("DELETE FROM mission_candidates");
            statement.execute("DELETE FROM spots");
        }
    }

    private Mission mission(String suffix) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("m" + suffix + (System.nanoTime() % 10_000L))
                .name("장소 " + suffix)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    private MissionCourse course(Mission anchor, Long userId) {
        return missionCourseRepository.save(MissionCourse.generate(
                anchor, userId, 3, "코스", null, null, null, 300, 90, 90
        ));
    }

    @Test
    void findExistingPairsReturnsSeq2AndSeq3MissionIdsForSameUserAndAnchor() {
        Mission anchor = mission("anchor");
        Mission m2 = mission("m2");
        Mission m3 = mission("m3");
        MissionCourse course = course(anchor, 1L);
        courseMissionRepository.save(CourseMission.of(course, anchor, 1));
        courseMissionRepository.save(CourseMission.of(course, m2, 2));
        courseMissionRepository.save(CourseMission.of(course, m3, 3));

        List<ExistingCoursePairProjection> pairs =
                courseMissionRepository.findExistingPairs(1L, anchor.getId());

        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0).getMission2Id()).isEqualTo(m2.getId());
        assertThat(pairs.get(0).getMission3Id()).isEqualTo(m3.getId());
    }

    @Test
    void findExistingPairsReturnsEmptyForDifferentUser() {
        Mission anchor = mission("anchor2");
        Mission m2 = mission("m2b");
        Mission m3 = mission("m3b");
        MissionCourse course = course(anchor, 1L);
        courseMissionRepository.save(CourseMission.of(course, anchor, 1));
        courseMissionRepository.save(CourseMission.of(course, m2, 2));
        courseMissionRepository.save(CourseMission.of(course, m3, 3));

        List<ExistingCoursePairProjection> pairs =
                courseMissionRepository.findExistingPairs(999L, anchor.getId());

        assertThat(pairs).isEmpty();
    }

    @Test
    void findLockRowsOnlyReturnsRowsFromInProgressCourses() {
        Mission anchor = mission("anchor3");
        Mission m2 = mission("m2c");
        MissionCourse course = course(anchor, 5L);
        courseMissionRepository.save(CourseMission.of(course, anchor, 1));
        courseMissionRepository.save(CourseMission.of(course, m2, 2));
        courseAttemptRepository.save(CourseAttempt.start(5L, course));

        List<CourseMissionLockRowProjection> rows = courseMissionRepository
                .findLockRowsForInProgressCourses(5L, List.of(m2.getId()));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getMissionId()).isEqualTo(m2.getId());
        assertThat(rows.get(0).getPrevMissionId()).isEqualTo(anchor.getId());
    }

    @Test
    void findByCreatedByUserIdOrderByCreatedAtDescReturnsOnlyThatUsersCourses() {
        Mission anchorA = mission("listA");
        Mission anchorB = mission("listB");
        missionCourseRepository.save(MissionCourse.generate(
                anchorA, 10L, 1, "A 코스", null, null, null, 100, 30, 30
        ));
        missionCourseRepository.save(MissionCourse.generate(
                anchorB, 20L, 1, "B 코스", null, null, null, 100, 30, 30
        ));

        List<MissionCourse> mine = missionCourseRepository.findByCreatedByUserIdOrderByCreatedAtDesc(10L);

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getName()).isEqualTo("A 코스");
    }
}
