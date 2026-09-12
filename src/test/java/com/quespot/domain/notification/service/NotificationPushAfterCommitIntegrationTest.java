package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.NotificationCommand;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.DeviceType;
import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// notify()가 커밋된 뒤에만 푸시가 나가고, 롤백되면 안 나가며, 무효 토큰은 REQUIRES_NEW로 실제 삭제되는지
// 실제 트랜잭션 경계로 검증한다. Mockito만으로는 AFTER_COMMIT을 재현할 수 없다.
@SpringBootTest
@Testcontainers
class NotificationPushAfterCommitIntegrationTest {

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

    @MockitoBean
    private FcmPushSender fcmPushSender;

    @Autowired private NotificationService notificationService;
    @Autowired private FcmTokenRepository fcmTokenRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private NotificationCommand command(Long userId) {
        return new NotificationCommand(userId, NotificationType.MISSION_RECOMMENDATION, "t", "b",
                NotificationReferenceType.MISSION, 1L);
    }

    @Test
    void sendsAfterCommitAndDeletesInvalidTokens() {
        Long userId = 9701L;
        fcmTokenRepository.save(FcmToken.register(userId, "keep-9701", DeviceType.ANDROID));
        fcmTokenRepository.save(FcmToken.register(userId, "gone-9701", DeviceType.IOS));
        when(fcmPushSender.send(anyList(), anyString(), any(), anyMap()))
                .thenReturn(new PushResult(1, List.of("gone-9701")));

        notificationService.notify(command(userId));

        verify(fcmPushSender, timeout(1000)).send(anyList(), eq("t"), eq("b"), anyMap());
        assertThat(fcmTokenRepository.findByToken("gone-9701")).isEmpty();
        assertThat(fcmTokenRepository.findByToken("keep-9701")).isPresent();
    }

    @Test
    void doesNotSendWhenTransactionRollsBack() {
        Long userId = 9702L;
        fcmTokenRepository.save(FcmToken.register(userId, "rb-9702", DeviceType.ANDROID));

        assertThatThrownBy(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    notificationService.notify(command(userId));
                    throw new IllegalStateException("force rollback");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("force rollback");

        verify(fcmPushSender, never()).send(anyList(), anyString(), any(), anyMap());
    }
}
