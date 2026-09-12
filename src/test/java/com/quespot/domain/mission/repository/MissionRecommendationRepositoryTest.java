package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.projection.RecommendedMissionProjection;
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
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@Testcontainers
class MissionRecommendationRepositoryTest {

    private static final long USER_ID = 9201L;

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
    private MissionRepository missionRepository;

    @Autowired
    private MissionAttemptRepository missionAttemptRepository;

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
    void recommendsPreferredCategoriesFirstAndExcludesCompletedAndInProgressMissions() {
        Mission foodNear = createMission("food-near", AppCategory.FOOD, MissionTemplate.FOOD_LOCATION, "37.5700");
        Mission foodFar = createMission("food-far", AppCategory.FOOD, MissionTemplate.FOOD_LOCATION, "37.5710");
        Mission natureNear = createMission("nature-near", AppCategory.NATURE, MissionTemplate.NATURE_LOCATION, "37.5800");
        Mission natureFar = createMission("nature-far", AppCategory.NATURE, MissionTemplate.NATURE_LOCATION, "37.5810");
        Mission history = createMission("history", AppCategory.HISTORY, MissionTemplate.HISTORY_LOCATION, "37.5900");
        Mission completed = createMission("completed", AppCategory.FOOD, MissionTemplate.FOOD_LOCATION, "37.6000");
        Mission inProgress = createMission("progress", AppCategory.NATURE, MissionTemplate.NATURE_LOCATION, "37.6100");

        MissionAttempt completedAttempt = MissionAttempt.start(USER_ID, completed);
        completedAttempt.complete(completed.getSnapshotLatitude(), completed.getSnapshotLongitude(), 80);
        missionAttemptRepository.save(completedAttempt);
        missionAttemptRepository.save(MissionAttempt.start(USER_ID, inProgress));

        List<RecommendedMissionProjection> rows = missionRepository.findRecommendedMissions(
                USER_ID,
                "FOOD,NATURE",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                1234L,
                null, null, null, null, null,
                20
        );

        assertThat(rows).extracting(RecommendedMissionProjection::getMissionId)
                .doesNotContain(completed.getId(), inProgress.getId());
        String firstCategory = rows.get(0).getCategory();
        String secondCategory = firstCategory.equals(MissionCategory.FOOD.name())
                ? MissionCategory.NATURE.name()
                : MissionCategory.FOOD.name();
        assertThat(rows.subList(0, 4)).extracting(
                        RecommendedMissionProjection::getCategory,
                        RecommendedMissionProjection::getCategoryRank
                )
                .containsExactly(
                        tuple(firstCategory, 1L),
                        tuple(secondCategory, 1L),
                        tuple(firstCategory, 2L),
                        tuple(secondCategory, 2L)
                );
        assertThat(rows.subList(0, 4)).extracting(RecommendedMissionProjection::getPreferenceRank)
                .containsOnly(0);
        assertThat(rows.get(4).getMissionId()).isEqualTo(history.getId());
        assertThat(rows.get(4).getPreferenceRank()).isEqualTo(1);
        assertThat(rows).extracting(RecommendedMissionProjection::getMissionId)
                .contains(foodNear.getId(), foodFar.getId(), natureNear.getId(), natureFar.getId());
    }

    private Mission createMission(
            String suffix,
            AppCategory appCategory,
            MissionTemplate template,
            String latitude
    ) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API)
                .sourceContentId("rec-" + suffix)
                .name("추천 장소 " + suffix)
                .latitude(new BigDecimal(latitude))
                .longitude(new BigDecimal("126.9780"))
                .appCategory(appCategory)
                .categoryMappingVersion(1)
                .showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, template, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }
}
