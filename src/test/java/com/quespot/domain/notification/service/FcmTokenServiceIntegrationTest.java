package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.DeviceType;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// 새 토큰은 FcmTokenWriter가 REQUIRES_NEW로 넣어 돌려주므로 registerToken()의 컨텍스트에선 detached다.
// 위치가 삽입 시점에 실제로 저장되는지는 Mockito로는 못 잡는다(#57 리뷰에서 발견된 버그의 회귀 테스트).
@SpringBootTest
@Testcontainers
class FcmTokenServiceIntegrationTest {

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

    @Autowired private FcmTokenService fcmTokenService;
    @Autowired private FcmTokenWriter fcmTokenWriter;
    @Autowired private FcmTokenRepository fcmTokenRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void persistsLocationOnFirstRegistration() {
        fcmTokenService.registerToken(9801L, new RegisterFcmTokenRequestDTO(
                "new-9801", DeviceType.ANDROID, new BigDecimal("37.5665"), new BigDecimal("126.9780")));

        FcmToken stored = fcmTokenRepository.findByToken("new-9801").orElseThrow();
        assertThat(stored.getLastLatitude()).isEqualByComparingTo("37.5665");
        assertThat(stored.getLastLongitude()).isEqualByComparingTo("126.9780");
        assertThat(stored.getLocatedAt()).isNotNull();
    }

    @Test
    void updatesLocationOnReRegistrationKeepsItWhenOmittedAndClearsItOnOwnerChange() {
        fcmTokenService.registerToken(9802L, new RegisterFcmTokenRequestDTO(
                "re-9802", DeviceType.IOS, new BigDecimal("37.1"), new BigDecimal("127.1")));

        fcmTokenService.registerToken(9802L, new RegisterFcmTokenRequestDTO(
                "re-9802", DeviceType.IOS, new BigDecimal("35.1"), new BigDecimal("129.0")));
        FcmToken moved = fcmTokenRepository.findByToken("re-9802").orElseThrow();
        assertThat(moved.getLastLatitude()).isEqualByComparingTo("35.1");

        fcmTokenService.registerToken(9802L, new RegisterFcmTokenRequestDTO(
                "re-9802", DeviceType.IOS, null, null));
        FcmToken kept = fcmTokenRepository.findByToken("re-9802").orElseThrow();
        assertThat(kept.getLastLatitude()).isEqualByComparingTo("35.1");

        fcmTokenService.registerToken(9803L, new RegisterFcmTokenRequestDTO(
                "re-9802", DeviceType.ANDROID, null, null));
        FcmToken reassigned = fcmTokenRepository.findByToken("re-9802").orElseThrow();
        assertThat(reassigned.getUserId()).isEqualTo(9803L);
        assertThat(reassigned.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(reassigned.getLastLatitude()).isNull();
        assertThat(reassigned.getLocatedAt()).isNull();
    }

    // 따닥 등록의 진 쪽 상황을 재현한다: 바깥 트랜잭션이 빈 결과로 스냅샷을 잡은 뒤 이긴 쪽(REQUIRES_NEW)이
    // 커밋하면, 같은 트랜잭션의 재조회는 REPEATABLE READ 때문에 그 행을 못 보지만
    // writer.reassignExisting()(새 트랜잭션)은 본다 — registerToken()의 fallback이 후자를 쓰는 이유.
    @Test
    void raceLoserSeesWinnerRowOnlyThroughFreshTransaction() {
        String token = "race-9804";
        RegisterFcmTokenRequestDTO loserRequest = new RegisterFcmTokenRequestDTO(
                token, DeviceType.ANDROID, new BigDecimal("37.5"), new BigDecimal("127.0"));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(fcmTokenRepository.findByToken(token)).isEmpty(); // 스냅샷 확정

            fcmTokenWriter.saveNewToken(9805L, new RegisterFcmTokenRequestDTO(token, DeviceType.IOS, null, null));

            assertThat(fcmTokenRepository.findByToken(token))
                    .as("같은 트랜잭션의 재조회는 옛 스냅샷이라 이긴 쪽 행을 못 본다")
                    .isEmpty();
            assertThat(fcmTokenWriter.reassignExisting(9804L, loserRequest))
                    .as("새 트랜잭션은 이긴 쪽 행을 보고 갱신한다")
                    .isPresent();
        });

        FcmToken stored = fcmTokenRepository.findByToken(token).orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(9804L);
        assertThat(stored.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(stored.getLastLatitude()).isEqualByComparingTo("37.5");
    }
}
