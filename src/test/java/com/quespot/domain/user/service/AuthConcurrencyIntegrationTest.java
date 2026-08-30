package com.quespot.domain.user.service;

import com.quespot.domain.user.dto.req.LoginRequestDTO;
import com.quespot.domain.user.dto.token.LoginResultDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.global.security.principal.AuthenticatedUser;
import com.quespot.global.security.provider.JwtTokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Testcontainers
class AuthConcurrencyIntegrationTest {

    private static final int REDIS_PORT = 6379;
    private static final String PASSWORD = "password1234!";
    private static final String JWT_SECRET = Base64.getEncoder().encodeToString(
            "test-jwt-secret-key-must-be-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)
    );

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("quespot_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:8.0-alpine")
    ).withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add("app.jwt.secret", () -> JWT_SECRET);
        registry.add("app.mail.verification-code-secret", () -> "integration-test-mail-secret");
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @MockitoSpyBean
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        userRepository.deleteAll();
    }

    @Test
    void withdrawalWaitsForLoginAndDeletesTheNewSession() throws Exception {
        User user = userRepository.saveAndFlush(User.createEmailUser(
                "member@quespot.test",
                passwordEncoder.encode(PASSWORD),
                "member"
        ));
        CountDownLatch sessionSaved = new CountDownLatch(1);
        CountDownLatch releaseLogin = new CountDownLatch(1);
        CountDownLatch withdrawalStarted = new CountDownLatch(1);
        AtomicReference<String> sessionId = new AtomicReference<>();
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Answer<Void> pauseAfterSavingSession = invocation -> {
            invocation.callRealMethod();
            JwtTokenPair tokenPair = invocation.getArgument(1);
            sessionId.set(tokenPair.sessionId());
            sessionSaved.countDown();

            if (!releaseLogin.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Login transaction was not released in time.");
            }
            return null;
        };
        doAnswer(pauseAfterSavingSession)
                .when(refreshTokenService)
                .saveRefreshToken(eq(user.getId()), any(JwtTokenPair.class));

        try {
            Future<LoginResultDTO> login = executorService.submit(() -> authService.login(
                    new LoginRequestDTO(user.getEmail(), PASSWORD)
            ));

            assertThat(sessionSaved.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Void> withdrawal = executorService.submit(() -> {
                withdrawalStarted.countDown();
                authService.withdraw(new AuthenticatedUser(user.getId(), user.getRole(), sessionId.get()));
                return null;
            });

            assertThat(withdrawalStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> withdrawal.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseLogin.countDown();
            login.get(5, TimeUnit.SECONDS);
            withdrawal.get(5, TimeUnit.SECONDS);

            User withdrawnUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(withdrawnUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(withdrawnUser.getDeletedAt()).isNotNull();
            assertThat(refreshTokenService.isSessionActive(user.getId(), sessionId.get())).isFalse();
        } finally {
            releaseLogin.countDown();
            executorService.shutdownNow();
        }
    }
}
