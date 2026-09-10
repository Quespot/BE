package com.quespot.domain.like.repository;

import com.quespot.domain.like.dto.LikedMissionRowDTO;
import com.quespot.domain.like.enums.LikeTargetType;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 같은 좋아요를 두 번 넣어도 예외 없이 행 1개, 없는 것을 지워도 예외 없음, 그리고
// FK 없는 likes를 `join ... on`으로 미션에 붙이는 JPQL이 실제로 동작하는지를
// 실DB로 검증한다 — Mockito로는 DB 제약·JPQL을 재현할 수 없다.
@SpringBootTest
@Testcontainers
class LikeRepositoryIntegrationTest {

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
    private LikeRepository likeRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private MissionCandidateRepository missionCandidateRepository;

    @Autowired
    private MissionRepository missionRepository;

    private Mission publishedMission(String name) {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("like" + (System.nanoTime() % 100_000_000_000_000L))
                .name(name)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());
        MissionCandidate candidate = missionCandidateRepository.save(
                MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1)
        );
        return missionRepository.save(Mission.publish(candidate));
    }

    @Test
    @Transactional
    void upsertTwiceLeavesSingleRowWithoutException() {
        likeRepository.upsert(9101L, "MISSION", 1L);
        likeRepository.upsert(9101L, "MISSION", 1L);

        assertThat(likeRepository.existsByUserIdAndTargetTypeAndTargetId(9101L, LikeTargetType.MISSION, 1L)).isTrue();
        assertThat(likeRepository.deleteByUserIdAndTargetTypeAndTargetId(9101L, LikeTargetType.MISSION, 1L)).isEqualTo(1);
    }

    @Test
    @Transactional
    void deleteMissingLikeReturnsZeroWithoutException() {
        long deleted = likeRepository.deleteByUserIdAndTargetTypeAndTargetId(9102L, LikeTargetType.COURSE, 999L);

        assertThat(deleted).isZero();
    }

    @Test
    @Transactional
    void findLikedMissionsJoinsWithoutForeignKeyAndSkipsDanglingTargets() {
        Mission liked = publishedMission("좋아요한 미션");
        likeRepository.upsert(9103L, "MISSION", liked.getId());
        likeRepository.upsert(9103L, "MISSION", 999_999L);   // 존재하지 않는 미션 — 목록에서 빠져야 함
        likeRepository.upsert(9104L, "MISSION", liked.getId()); // 다른 사용자

        List<LikedMissionRowDTO> rows = likeRepository.findLikedMissions(9103L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).mission().getId()).isEqualTo(liked.getId());
        assertThat(rows.get(0).likedAt()).isNotNull();
        assertThat(likeRepository.findLikedCourses(9103L)).isEmpty();
    }
}
