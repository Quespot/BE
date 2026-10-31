package com.quespot.domain.mission.service;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.res.CourseAttemptListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResultResponseDTO;
import com.quespot.domain.mission.dto.res.VerificationGuideResponseDTO;
import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// #67 회귀 테스트. open-in-view: false라 서비스의 readOnly 트랜잭션이 끝난 뒤
// 컨트롤러가 MissionConverter로 지연 로딩 연관관계(attempt.getMission(),
// attempt.getCourse())를 타면 LazyInitializationException이 났다. 이 테스트는
// 컨트롤러와 똑같이 트랜잭션 밖에서 서비스 → 컨버터를 호출한다 — 테스트 메서드에
// @Transactional을 붙이면 세션이 열려 있어 버그가 재현되지 않으니 붙이지 말 것.
@SpringBootTest
@Testcontainers
class AttemptDetailConversionIntegrationTest {

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
    private MissionAttemptService missionAttemptService;

    @Autowired
    private CourseAttemptService courseAttemptService;

    @Autowired
    private MissionAttemptRepository missionAttemptRepository;

    @Autowired
    private CourseAttemptRepository courseAttemptRepository;

    @Autowired
    private MissionCourseRepository missionCourseRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private MissionCandidateRepository missionCandidateRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private DataSource dataSource;

    // active_key 생성 컬럼 — MissionAttempt/CourseAttempt 상단 주석 참고.
    @BeforeEach
    void applyGeneratedColumns() throws SQLException {
        applyGeneratedColumn("mission_attempts", "mission_id");
        applyGeneratedColumn("course_attempts", "course_id");
    }

    private void applyGeneratedColumn(String table, String refColumn) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE %s
                    MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
                        CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', %s) END
                    ) STORED
                    """.formatted(table, refColumn));
        } catch (SQLException ignored) {
            // 같은 컨테이너를 재사용하는 다른 테스트가 이미 적용한 경우
        }
    }

    private Mission createPublishedMission(String suffix) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("ad" + suffix + (System.nanoTime() % 100_000_000_000L))
                .name("변환 테스트 스팟 " + suffix)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    @Test
    void attemptFetchedOutsideTransactionCanBeConvertedToVerificationGuide() {
        Long userId = 9301L;
        Mission mission = createPublishedMission("guide");
        MissionAttempt saved = missionAttemptRepository.save(MissionAttempt.start(userId, mission));

        MissionAttempt attempt = missionAttemptService.getAttempt(userId, saved.getId());

        VerificationGuideResponseDTO guide = MissionConverter.toVerificationGuideResponse(attempt);
        assertThat(guide.attemptId()).isEqualTo(saved.getId());
        assertThat(guide.targetLatitude()).isEqualByComparingTo(new BigDecimal("37.5665"));
        assertThat(guide.targetLongitude()).isEqualByComparingTo(new BigDecimal("126.9780"));
        assertThat(guide.radiusMeters()).isEqualTo(MissionArrivalService.RADIUS_METERS);

        // 같은 getAttempt를 쓰는 단건 조회·완료 결과 컨버터도 함께 확인한다.
        MissionAttemptResponseDTO single = MissionConverter.toAttemptResponse(attempt);
        assertThat(single.missionId()).isEqualTo(mission.getId());
        assertThat(single.missionTitle()).isEqualTo(mission.getTitle());
        MissionAttemptResultResponseDTO result = MissionConverter.toAttemptResultResponse(attempt, null);
        assertThat(result.missionTitle()).isEqualTo(mission.getTitle());
    }

    @Test
    void inProgressCourseAttemptsFetchedOutsideTransactionCanBeConvertedToList() {
        Long userId = 9302L;
        Mission anchor = createPublishedMission("course");
        MissionCourse course = missionCourseRepository.save(MissionCourse.generate(
                anchor, userId, 3, "변환 테스트 코스", "설명", null, "11110", 100, 30, 60
        ));
        CourseAttempt saved = courseAttemptRepository.save(CourseAttempt.start(userId, course));

        List<CourseAttempt> attempts = courseAttemptService.getInProgressList(userId);

        CourseAttemptListResponseDTO response = MissionConverter.toCourseAttemptListResponse(attempts);
        assertThat(response.attempts()).hasSize(1);
        assertThat(response.attempts().get(0).courseAttemptId()).isEqualTo(saved.getId());
        assertThat(response.attempts().get(0).courseId()).isEqualTo(course.getId());
        assertThat(response.attempts().get(0).courseName()).isEqualTo("변환 테스트 코스");
    }
}
