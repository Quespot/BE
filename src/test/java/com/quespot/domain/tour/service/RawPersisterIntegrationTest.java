package com.quespot.domain.tour.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 실DB(Testcontainers MySQL) 기반. Mockito 단위 테스트는 실제 Hibernate
// 세션/트랜잭션 매니저를 거치지 않으므로, 이 클래스가 고치는 버그
// (REQUIRES_NEW 트랜잭션이 캐치된 예외로도 rollback-only가 되는 문제)를
// 절대 잡아낼 수 없다. 이 테스트가 그 유일한 안전망이다.
@SpringBootTest
@Testcontainers
class RawPersisterIntegrationTest {

    private static final String OPERATION = "raw-persister-integration-test";

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
    private RawPersister rawPersister;

    @Autowired
    private TourContentRawRepository tourContentRawRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanUp() {
        tourContentRawRepository.findAll().stream()
                .filter(raw -> OPERATION.equals(raw.getOperation()))
                .forEach(tourContentRawRepository::delete);
    }

    @Test
    void duplicateMidBatchDoesNotPreventOtherItemsFromCommitting() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {"contentid":"row-1","modifiedtime":"20260101000000","showflag":"1"}
                """);
        JsonNode duplicateOfFirst = objectMapper.readTree("""
                {"contentid":"row-1","modifiedtime":"20260101000000","showflag":"1"}
                """);
        JsonNode second = objectMapper.readTree("""
                {"contentid":"row-2","modifiedtime":"20260101000000","showflag":"1"}
                """);

        rawPersister.saveAll(OPERATION, List.of(first, duplicateOfFirst, second));

        long savedCount = tourContentRawRepository.findAll().stream()
                .filter(raw -> OPERATION.equals(raw.getOperation()))
                .count();
        assertThat(savedCount).isEqualTo(2);
    }
}
