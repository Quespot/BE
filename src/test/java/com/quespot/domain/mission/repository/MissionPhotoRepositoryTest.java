package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
class MissionPhotoRepositoryTest {

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

    @Autowired private MissionPhotoRepository missionPhotoRepository;
    @Autowired private MissionAttemptRepository missionAttemptRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private MissionCandidateRepository missionCandidateRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM mission_photos");
            statement.execute("DELETE FROM mission_attempts");
            statement.execute("DELETE FROM missions");
            statement.execute("DELETE FROM mission_candidates");
            statement.execute("DELETE FROM spots");
        }
    }

    private MissionPhoto createPhotoFor(Long userId, String suffix) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("ph" + suffix + (System.nanoTime() % 10_000L))
                .name("아카이브 스팟 " + suffix)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        Mission mission = missionRepository.save(Mission.publish(candidate));
        MissionAttempt attempt = MissionAttempt.start(userId, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        attempt = missionAttemptRepository.save(attempt);
        return missionPhotoRepository.save(MissionPhoto.record(
                attempt, "missions/" + userId + "/" + suffix + ".jpg",
                null, new BigDecimal("37.5665"), new BigDecimal("126.9780"), null
        ));
    }

    @Test
    void findArchivePageReturnsOnlyGivenUsersPhotosOrderedNewestFirst() {
        createPhotoFor(9001L, "a");
        MissionPhoto other = createPhotoFor(9002L, "other");
        MissionPhoto p2 = createPhotoFor(9001L, "b");
        MissionPhoto p3 = createPhotoFor(9001L, "c");

        List<MissionPhoto> page = missionPhotoRepository.findArchivePage(9001L, null, null, PageRequest.of(0, 10));

        assertThat(page).hasSize(3);
        assertThat(page).extracting(MissionPhoto::getId).doesNotContain(other.getId());
        // 최신순(id 역순) — p3가 가장 나중에 만들어졌으므로 먼저 나와야 한다.
        assertThat(page.get(0).getId()).isEqualTo(p3.getId());
        assertThat(page.get(1).getId()).isEqualTo(p2.getId());
    }

    @Test
    void findArchivePageContinuesFromCursorWithoutOverlapOrGap() {
        MissionPhoto p1 = createPhotoFor(9003L, "a");
        MissionPhoto p2 = createPhotoFor(9003L, "b");
        MissionPhoto p3 = createPhotoFor(9003L, "c");
        MissionPhoto p4 = createPhotoFor(9003L, "d");

        List<MissionPhoto> firstPage = missionPhotoRepository.findArchivePage(9003L, null, null, PageRequest.of(0, 2));
        assertThat(firstPage).extracting(MissionPhoto::getId).containsExactly(p4.getId(), p3.getId());

        MissionPhoto cursorFrom = firstPage.get(firstPage.size() - 1);
        List<MissionPhoto> secondPage = missionPhotoRepository.findArchivePage(
                9003L, cursorFrom.getCreatedAt(), cursorFrom.getId(), PageRequest.of(0, 2)
        );
        assertThat(secondPage).extracting(MissionPhoto::getId).containsExactly(p2.getId(), p1.getId());
    }

    private void pinCreatedAt(List<MissionPhoto> photos, java.time.LocalDateTime createdAt) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("UPDATE mission_photos SET created_at = ? WHERE id = ?")) {
            for (MissionPhoto photo : photos) {
                statement.setObject(1, createdAt);
                statement.setLong(2, photo.getId());
                statement.executeUpdate();
            }
        }
    }

    @Test
    void findArchivePageBreaksTiesByIdWhenCreatedAtIsIdentical() throws SQLException {
        // 4건 전부 같은 createdAt으로 고정한다 — createdAt 조건만으로는 전혀
        // 구분이 안 되는 상태를 강제로 만들어서, id desc 타이브레이크 하나로만
        // 정확히 이어지는지(중복·누락 없이) 검증한다. 이걸 안 하면 실제로는
        // OR (mp.createdAt = :cursorCreatedAt and mp.id < :cursorId) 절이
        // 통째로 빠져도 이 테스트는 그냥 통과해버린다(각 호출 사이 실제 시간이
        // 흘러 createdAt이 자연히 달라지기 때문).
        MissionPhoto p1 = createPhotoFor(9005L, "a");
        MissionPhoto p2 = createPhotoFor(9005L, "b");
        MissionPhoto p3 = createPhotoFor(9005L, "c");
        MissionPhoto p4 = createPhotoFor(9005L, "d");
        java.time.LocalDateTime tiedAt = java.time.LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        pinCreatedAt(List.of(p1, p2, p3, p4), tiedAt);

        List<MissionPhoto> firstPage = missionPhotoRepository.findArchivePage(9005L, null, null, PageRequest.of(0, 2));
        assertThat(firstPage).extracting(MissionPhoto::getId).containsExactly(p4.getId(), p3.getId());

        MissionPhoto cursorFrom = firstPage.get(firstPage.size() - 1);
        List<MissionPhoto> secondPage = missionPhotoRepository.findArchivePage(
                9005L, cursorFrom.getCreatedAt(), cursorFrom.getId(), PageRequest.of(0, 2)
        );
        assertThat(secondPage).extracting(MissionPhoto::getId).containsExactly(p2.getId(), p1.getId());
    }

    @Test
    void findArchivePageFetchesAttemptAndMissionEagerlyToAvoidNPlusOne() {
        createPhotoFor(9004L, "a");

        List<MissionPhoto> page = missionPhotoRepository.findArchivePage(9004L, null, null, PageRequest.of(0, 10));

        // join fetch가 없으면 이 호출들이 지연 로딩 세션 밖에서 예외를 던진다 —
        // 여기서는 트랜잭션 안이라 예외는 안 나지만, join fetch 여부 자체는
        // Hibernate 통계 대신 "조회 결과가 이미 초기화돼 있는지"로 확인한다.
        assertThat(org.hibernate.Hibernate.isInitialized(page.get(0).getAttempt())).isTrue();
        assertThat(org.hibernate.Hibernate.isInitialized(page.get(0).getAttempt().getMission())).isTrue();
    }
}
