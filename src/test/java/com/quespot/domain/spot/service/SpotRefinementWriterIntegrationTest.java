package com.quespot.domain.spot.service;

import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import com.quespot.domain.tour.entity.TourContentRaw;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 실DB(Testcontainers MySQL) 기반. RawPersisterIntegrationTest와 같은
// 이유로 존재한다 — refineChunk 내부에서 한 건이 실패해도 나머지 건이
// 커밋되는지는 실제 트랜잭션 매니저 없이는 검증할 수 없다.
@SpringBootTest
@Testcontainers
class SpotRefinementWriterIntegrationTest {

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
    private SpotRefinementWriter spotRefinementWriter;

    @Autowired
    private SpotRepository spotRepository;

    @AfterEach
    void cleanUp() {
        spotRepository.findAll().stream()
                .filter(spot -> spot.getSourceContentId() != null && spot.getSourceContentId().startsWith("integration-"))
                .forEach(spotRepository::delete);
    }

    private TourContentRaw rawOf(String contentId, String payload) {
        return TourContentRaw.create(contentId, "spot-refinement-integration-test", payload,
                LocalDateTime.of(2026, 1, 1, 0, 0), true);
    }

    private String payload(String contentId) {
        return """
                {"contentid":"%s","contenttypeid":"12","title":"통합테스트 스팟",
                "addr1":"서울특별시 종로구","addr2":"","mapx":"127.0","mapy":"37.5",
                "firstimage":"img.jpg","firstimage2":"thumb.jpg","showflag":"1",
                "cpyrhtDivCd":"Type3","lDongRegnCd":"11","lDongSignguCd":"110",
                "lclsSystm1":"VE","lclsSystm2":"VE01","lclsSystm3":"VE010100"}
                """.formatted(contentId);
    }

    @Test
    void oneFailingRowDoesNotPreventOtherRowsInChunkFromCommitting() {
        TourContentRaw malformed = rawOf("integration-bad", "not-json");
        TourContentRaw good1 = rawOf("integration-1", payload("integration-1"));
        TourContentRaw good2 = rawOf("integration-2", payload("integration-2"));

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(malformed, good1, good2));

        assertThat(summary).isEqualTo(new RefinementSummary(2, 0, 0, 0, 1));
        assertThat(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "integration-1")).isPresent();
        assertThat(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "integration-2")).isPresent();
    }
}
