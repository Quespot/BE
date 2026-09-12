package com.quespot.domain.notification.repository;

import com.quespot.domain.notification.entity.Notification;
import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class NotificationRepositoryIntegrationTest {

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
    private NotificationRepository notificationRepository;

    private Notification save(Long userId, String title) {
        return notificationRepository.save(Notification.create(
                userId, NotificationType.MISSION_RECOMMENDATION, title, null, NotificationReferenceType.MISSION, 1L));
    }

    @Test
    @Transactional
    void pagesNewestFirstWithCursor() {
        Long userId = 9601L;
        Notification n1 = save(userId, "1");
        Notification n2 = save(userId, "2");
        Notification n3 = save(userId, "3");
        save(9602L, "other");

        List<Notification> first = notificationRepository.findPage(userId, null, PageRequest.of(0, 2));
        List<Notification> second = notificationRepository.findPage(userId, n2.getId(), PageRequest.of(0, 2));

        assertThat(first).extracting(Notification::getId).containsExactly(n3.getId(), n2.getId());
        assertThat(second).extracting(Notification::getId).containsExactly(n1.getId());
    }

    @Test
    @Transactional
    void countsUnreadAndMarksRead() {
        Long userId = 9603L;
        Notification a = save(userId, "a");
        save(userId, "b");

        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(userId)).isEqualTo(2);
        assertThat(notificationRepository.markAsRead(a.getId(), userId, LocalDateTime.now())).isEqualTo(1);
        assertThat(notificationRepository.markAsRead(a.getId(), userId, LocalDateTime.now())).isZero();
        assertThat(notificationRepository.markAsRead(a.getId(), 9604L, LocalDateTime.now())).isZero();
        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(userId)).isEqualTo(1);
        assertThat(notificationRepository.markAllAsRead(userId, LocalDateTime.now())).isEqualTo(1);
        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(userId)).isZero();
    }

    @Test
    @Transactional
    void detectsNotificationSentSince() {
        Long userId = 9605L;
        save(userId, "today");

        assertThat(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, NotificationType.MISSION_RECOMMENDATION, LocalDateTime.now().minusMinutes(1))).isTrue();
        assertThat(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, NotificationType.MISSION_RECOMMENDATION, LocalDateTime.now().plusMinutes(1))).isFalse();
    }
}
