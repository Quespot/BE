package com.quespot.domain.like.repository;

import com.quespot.domain.like.enums.LikeTargetType;
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

import static org.assertj.core.api.Assertions.assertThat;

// 같은 좋아요를 두 번 넣어도 예외 없이 행 1개, 없는 것을 지워도 예외 없음을
// 실DB(UNIQUE + ON DUPLICATE KEY UPDATE)로 검증한다 — Mockito로는 DB 제약을
// 재현할 수 없다.
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
    private SavedSpotRepository savedSpotRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Test
    @Transactional
    void upsertTwiceLeavesSingleRowWithoutException() {
        likeRepository.upsert(9101L, "MISSION", 1L);
        likeRepository.upsert(9101L, "MISSION", 1L);

        assertThat(likeRepository.existsByUserIdAndTargetTypeAndTargetId(9101L, LikeTargetType.MISSION, 1L)).isTrue();
        assertThat(likeRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional
    void deleteMissingLikeReturnsZeroWithoutException() {
        long deleted = likeRepository.deleteByUserIdAndTargetTypeAndTargetId(9102L, LikeTargetType.SPOT, 999L);

        assertThat(deleted).isZero();
    }

    @Test
    @Transactional
    void savedSpotUpsertTwiceLeavesSingleRow() {
        Spot spot = spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("like-it-1").name("저장 스팟")
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build());

        savedSpotRepository.upsert(9103L, spot.getId());
        savedSpotRepository.upsert(9103L, spot.getId());

        assertThat(savedSpotRepository.existsByUserIdAndSpot_Id(9103L, spot.getId())).isTrue();
        assertThat(savedSpotRepository.deleteByUserIdAndSpot_Id(9103L, spot.getId())).isEqualTo(1);
    }
}
