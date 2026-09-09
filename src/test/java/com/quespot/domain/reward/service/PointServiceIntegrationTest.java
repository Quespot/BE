package com.quespot.domain.reward.service;

import com.quespot.domain.reward.repository.PointTransactionRepository;
import com.quespot.domain.reward.repository.UserPointRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// point_transactions의 UNIQUE(user_id, type, reference_type, reference_id)가
// 실제로 중복 지급을 막는지 실DB로 검증한다 — Mockito로는 DB 제약을
// 재현할 수 없다.
@SpringBootTest
@Testcontainers
class PointServiceIntegrationTest {

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
    private PointService pointService;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Test
    void firstCreditCreatesUserPointRowAndSecondCreditWithSameReferenceIsBlocked() {
        pointService.credit(9001L, 100, "MISSION_REWARD", "MISSION_ATTEMPT", 1L, "미션 완료 보상");

        assertThat(userPointRepository.findById(9001L)).isPresent();
        assertThat(userPointRepository.findById(9001L).get().getBalance()).isEqualTo(100);
        assertThat(pointTransactionRepository.findAll()).hasSize(1);

        assertThatThrownBy(() ->
                pointService.credit(9001L, 100, "MISSION_REWARD", "MISSION_ATTEMPT", 1L, "미션 완료 보상")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
