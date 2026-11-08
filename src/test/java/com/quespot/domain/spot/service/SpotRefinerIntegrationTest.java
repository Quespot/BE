package com.quespot.domain.spot.service;

import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// 실DB(Testcontainers MySQL) 기반. 정제 대상 선별 쿼리(findRefinementTargetIds)가
// content_id별 최신 원본 중 spot과 달라진 것만 고르는지 검증한다.
@SpringBootTest
@Testcontainers
class SpotRefinerIntegrationTest {

    private static final String OPERATION = "areaBasedSyncList2";
    private static final LocalDateTime OLDER = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime NEWER = LocalDateTime.of(2026, 6, 1, 0, 0);

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
    private SpotRefiner spotRefiner;

    @Autowired
    private TourContentRawRepository tourContentRawRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private CategoryResolver categoryResolver;

    @AfterEach
    void cleanUp() {
        spotRepository.deleteAll(spotRepository.findAll().stream()
                .filter(spot -> spot.getSourceContentId() != null && spot.getSourceContentId().startsWith("it-"))
                .toList());
        tourContentRawRepository.deleteAll(tourContentRawRepository.findAll().stream()
                .filter(raw -> raw.getContentId().startsWith("it-"))
                .toList());
    }

    @Test
    void refinesOnlyLatestRawThatDiffersFromSpot() {
        // it-new: spot 없음, 원본 2건 중 최신 버전으로 생성돼야 한다
        saveRaw("it-new", OLDER, "옛 이름");
        saveRaw("it-new", NEWER, "새 이름");
        // it-same: spot의 원천 수정 시각·분류 버전이 최신 원본과 같다 → 대상 아님
        saveRaw("it-same", NEWER, "그대로");
        saveSpot("it-same", NEWER, categoryResolver.getCurrentVersion());
        // it-changed: spot이 이전 버전 → 갱신
        saveRaw("it-changed", NEWER, "갱신된 이름");
        saveSpot("it-changed", OLDER, categoryResolver.getCurrentVersion());

        RefinementSummary summary = spotRefiner.refine();

        assertThat(summary).isEqualTo(new RefinementSummary(1, 1, 0, 1, 0));
        Spot created = spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "it-new").orElseThrow();
        assertThat(created.getName()).isEqualTo("새 이름");
        assertThat(created.getSourceModifiedAt()).isEqualTo(NEWER);
        Spot updated = spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "it-changed").orElseThrow();
        assertThat(updated.getName()).isEqualTo("갱신된 이름");
    }

    @Test
    void refinesUnchangedSourceAgainWhenCategoryMappingVersionIsStale() {
        saveRaw("it-stale", NEWER, "분류 재적용");
        saveSpot("it-stale", NEWER, categoryResolver.getCurrentVersion() - 1);

        RefinementSummary summary = spotRefiner.refine();

        assertThat(summary.updated()).isEqualTo(1);
    }

    private void saveRaw(String contentId, LocalDateTime modifiedTime, String title) {
        String payload = """
                {"contentid":"%s","contenttypeid":"12","title":"%s",
                "addr1":"서울특별시 종로구","addr2":"","mapx":"126.98","mapy":"37.57",
                "firstimage":"img.jpg","firstimage2":"thumb.jpg","showflag":"1",
                "cpyrhtDivCd":"Type3","lDongRegnCd":"11","lDongSignguCd":"110",
                "lclsSystm1":"HS","lclsSystm2":"HS01","lclsSystm3":"HS010100"}
                """.formatted(contentId, title);
        tourContentRawRepository.save(TourContentRaw.create(contentId, OPERATION, payload, modifiedTime, true));
    }

    private void saveSpot(String contentId, LocalDateTime sourceModifiedAt, int categoryMappingVersion) {
        spotRepository.save(Spot.builder()
                .source(SpotSource.TOUR_API)
                .sourceContentId(contentId)
                .name("기존 이름")
                .latitude(new BigDecimal("37.57"))
                .longitude(new BigDecimal("126.98"))
                .appCategory(AppCategory.HISTORY)
                .categoryMappingVersion(categoryMappingVersion)
                .showFlag(true)
                .sourceModifiedAt(sourceModifiedAt)
                .build());
    }
}
