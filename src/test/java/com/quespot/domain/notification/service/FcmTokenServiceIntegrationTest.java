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
    @Autowired private FcmTokenRepository fcmTokenRepository;

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
    void updatesLocationOnReRegistrationAndKeepsItWhenOmitted() {
        fcmTokenService.registerToken(9802L, new RegisterFcmTokenRequestDTO(
                "re-9802", DeviceType.IOS, new BigDecimal("37.1"), new BigDecimal("127.1")));

        fcmTokenService.registerToken(9802L, new RegisterFcmTokenRequestDTO(
                "re-9802", DeviceType.IOS, new BigDecimal("35.1"), new BigDecimal("129.0")));
        FcmToken moved = fcmTokenRepository.findByToken("re-9802").orElseThrow();
        assertThat(moved.getLastLatitude()).isEqualByComparingTo("35.1");

        fcmTokenService.registerToken(9803L, new RegisterFcmTokenRequestDTO(
                "re-9802", DeviceType.ANDROID, null, null));
        FcmToken reassigned = fcmTokenRepository.findByToken("re-9802").orElseThrow();
        assertThat(reassigned.getUserId()).isEqualTo(9803L);
        assertThat(reassigned.getDeviceType()).isEqualTo(DeviceType.ANDROID);
        assertThat(reassigned.getLastLatitude()).isEqualByComparingTo("35.1");
    }
}
