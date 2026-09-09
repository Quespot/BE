package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.projection.ArchiveFeedRowProjection;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ArchivePhotoRepositoryTest {

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

    @Autowired private ArchivePhotoRepository archivePhotoRepository;
    @Autowired private MissionPhotoRepository missionPhotoRepository;
    @Autowired private MissionAttemptRepository missionAttemptRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private MissionCandidateRepository missionCandidateRepository;
    @Autowired private SpotRepository spotRepository;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM archive_photos");
            statement.execute("DELETE FROM mission_photos");
            statement.execute("DELETE FROM mission_attempts");
            statement.execute("DELETE FROM missions");
            statement.execute("DELETE FROM mission_candidates");
            statement.execute("DELETE FROM spots");
        }
    }

    private MissionPhoto createMissionPhotoFor(Long userId, String suffix) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("f" + suffix + (System.nanoTime() % 10_000L))
                .name("피드 스팟 " + suffix)
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
                attempt, "missions/" + userId + "/" + suffix + ".jpg", null,
                new BigDecimal("37.5665"), new BigDecimal("126.9780"), null
        ));
    }

    private ArchivePhoto createArchivePhotoFor(Long userId, String suffix) {
        return archivePhotoRepository.save(
                ArchivePhoto.upload(userId, "archives/" + userId + "/" + suffix + ".jpg", "자유 업로드 " + suffix)
        );
    }

    @Test
    void findFeedPageReturnsOnlyGivenUsersRowsFromBothSourcesOrderedNewestFirst() {
        createMissionPhotoFor(9101L, "m1");
        ArchivePhoto other = createArchivePhotoFor(9102L, "other");
        ArchivePhoto a1 = createArchivePhotoFor(9101L, "a1");
        MissionPhoto m2 = createMissionPhotoFor(9101L, "m2");

        List<ArchiveFeedRowProjection> page = archivePhotoRepository.findFeedPage(9101L, null, null, null, 10);

        assertThat(page).hasSize(3);
        // mission_photos.id와 archive_photos.id는 서로 독립된 시퀀스라 값이
        // 겹칠 수 있다 — id만으로는 "다른 유저의 행이 안 섞였는지"를 증명할 수
        // 없으므로 (id, source) 쌍으로 비교한다.
        assertThat(page).extracting(ArchiveFeedRowProjection::getId, ArchiveFeedRowProjection::getSource)
                .doesNotContain(org.assertj.core.groups.Tuple.tuple(other.getId(), "ARCHIVE"));
        // 최신순 — m2가 가장 나중에 만들어졌으므로 먼저 나와야 한다.
        assertThat(page.get(0).getId()).isEqualTo(m2.getId());
        assertThat(page.get(0).getSource()).isEqualTo("MISSION");
        assertThat(page.get(1).getId()).isEqualTo(a1.getId());
        assertThat(page.get(1).getSource()).isEqualTo("ARCHIVE");
    }

    @Test
    void findFeedPageFillsMissionFieldsOnlyForMissionSource() {
        MissionPhoto missionPhoto = createMissionPhotoFor(9106L, "m");
        createArchivePhotoFor(9106L, "a");

        List<ArchiveFeedRowProjection> page = archivePhotoRepository.findFeedPage(9106L, null, null, null, 10);

        ArchiveFeedRowProjection missionRow = page.stream()
                .filter(row -> row.getId().equals(missionPhoto.getId()) && "MISSION".equals(row.getSource()))
                .findFirst().orElseThrow();
        assertThat(missionRow.getMissionId()).isNotNull();
        assertThat(missionRow.getMissionTitle()).isNotNull();
        assertThat(missionRow.getMissionCategory()).isNotNull();
        assertThat(missionRow.getCompletedAt()).isNotNull();

        ArchiveFeedRowProjection archiveRow = page.stream()
                .filter(row -> "ARCHIVE".equals(row.getSource()))
                .findFirst().orElseThrow();
        assertThat(archiveRow.getMissionId()).isNull();
        assertThat(archiveRow.getMissionTitle()).isNull();
        assertThat(archiveRow.getMissionCategory()).isNull();
        assertThat(archiveRow.getCompletedAt()).isNull();
    }

    @Test
    void findFeedPageContinuesFromCursorWithoutOverlapOrGap() {
        MissionPhoto p1 = createMissionPhotoFor(9103L, "a");
        ArchivePhoto p2 = createArchivePhotoFor(9103L, "b");
        MissionPhoto p3 = createMissionPhotoFor(9103L, "c");
        ArchivePhoto p4 = createArchivePhotoFor(9103L, "d");

        List<ArchiveFeedRowProjection> firstPage =
                archivePhotoRepository.findFeedPage(9103L, null, null, null, 2);
        assertThat(firstPage).extracting(ArchiveFeedRowProjection::getId).containsExactly(p4.getId(), p3.getId());

        ArchiveFeedRowProjection cursorFrom = firstPage.get(firstPage.size() - 1);
        List<ArchiveFeedRowProjection> secondPage = archivePhotoRepository.findFeedPage(
                9103L, cursorFrom.getCreatedAt(), cursorFrom.getSource(), cursorFrom.getId(), 2
        );
        assertThat(secondPage).extracting(ArchiveFeedRowProjection::getId).containsExactly(p2.getId(), p1.getId());
    }

    private void pinCreatedAt(String table, String column, List<Long> ids, LocalDateTime createdAt) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("UPDATE " + table + " SET " + column + " = ? WHERE id = ?")) {
            for (Long id : ids) {
                statement.setObject(1, createdAt);
                statement.setLong(2, id);
                statement.executeUpdate();
            }
        }
    }

    @Test
    void findFeedPageBreaksTiesBySourceThenIdWhenCreatedAtIsIdentical() throws SQLException {
        // mission_photos/archive_photos 양쪽 다 같은 createdAt으로 고정한다 —
        // createdAt만으로는 전혀 구분이 안 되는 상태를 강제로 만들어서, source
        // asc(ARCHIVE 먼저) + id desc 타이브레이크가 실제로 중복·누락 없이
        // 정확히 이어지는지 검증한다. 이걸 안 하면 타이브레이크 조건이 통째로
        // 빠져도 각 호출 사이 실제 시간이 흘러 createdAt이 자연히 달라지므로
        // 이 테스트 없이는 통과해버린다.
        MissionPhoto m1 = createMissionPhotoFor(9107L, "m1");
        MissionPhoto m2 = createMissionPhotoFor(9107L, "m2");
        ArchivePhoto a1 = createArchivePhotoFor(9107L, "a1");
        ArchivePhoto a2 = createArchivePhotoFor(9107L, "a2");
        LocalDateTime tiedAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        pinCreatedAt("mission_photos", "created_at", List.of(m1.getId(), m2.getId()), tiedAt);
        pinCreatedAt("archive_photos", "created_at", List.of(a1.getId(), a2.getId()), tiedAt);

        // 정렬 순서: createdAt(전부 동일) -> source asc(ARCHIVE < MISSION) -> id desc
        // 기대 순서: a2, a1, m2, m1
        List<ArchiveFeedRowProjection> firstPage =
                archivePhotoRepository.findFeedPage(9107L, null, null, null, 2);
        assertThat(firstPage).extracting(ArchiveFeedRowProjection::getId, ArchiveFeedRowProjection::getSource)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(a2.getId(), "ARCHIVE"),
                        org.assertj.core.groups.Tuple.tuple(a1.getId(), "ARCHIVE")
                );

        ArchiveFeedRowProjection cursorFrom = firstPage.get(firstPage.size() - 1);
        List<ArchiveFeedRowProjection> secondPage = archivePhotoRepository.findFeedPage(
                9107L, cursorFrom.getCreatedAt(), cursorFrom.getSource(), cursorFrom.getId(), 2
        );
        assertThat(secondPage).extracting(ArchiveFeedRowProjection::getId, ArchiveFeedRowProjection::getSource)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(m2.getId(), "MISSION"),
                        org.assertj.core.groups.Tuple.tuple(m1.getId(), "MISSION")
                );
    }
}
