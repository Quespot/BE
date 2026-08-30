package com.quespot.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.global.security.filter.JwtAuthenticationFilter;
import com.quespot.global.security.handler.SecurityErrorResponseWriter;
import com.quespot.global.security.provider.JwtTokenPair;
import com.quespot.global.security.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class RefreshTokenRedisIntegrationTest {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "quespot:auth:refresh";
    private static final String USER_SESSION_KEY_PREFIX = "quespot:auth:user-sessions";
    private static final int REDIS_PORT = 6379;
    private static final String JWT_SECRET = Base64.getEncoder().encodeToString(
            "test-jwt-secret-key-must-be-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)
    );

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:8.0-alpine")
    ).withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate stringRedisTemplate;

    private RefreshTokenService refreshTokenService;
    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
        connectionFactory.afterPropertiesSet();

        stringRedisTemplate = new StringRedisTemplate(connectionFactory);
        stringRedisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void tearDownRedis() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
        refreshTokenService = new RefreshTokenService(stringRedisTemplate);
        jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, 30L, 14L);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                refreshTokenService,
                new SecurityErrorResponseWriter(new ObjectMapper())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deletesAllSessionsForOnlyTheWithdrawnUser() {
        Long withdrawnUserId = 1L;
        Long activeUserId = 2L;
        JwtTokenPair firstSession = tokenPair("session-a", "refresh-a");
        JwtTokenPair secondSession = tokenPair("session-b", "refresh-b");
        JwtTokenPair otherUserSession = tokenPair("session-c", "refresh-c");

        refreshTokenService.saveRefreshToken(withdrawnUserId, firstSession);
        refreshTokenService.saveRefreshToken(withdrawnUserId, secondSession);
        refreshTokenService.saveRefreshToken(activeUserId, otherUserSession);

        refreshTokenService.deleteAllSessions(withdrawnUserId);

        assertThat(refreshTokenService.isSessionActive(withdrawnUserId, firstSession.sessionId())).isFalse();
        assertThat(refreshTokenService.isSessionActive(withdrawnUserId, secondSession.sessionId())).isFalse();
        assertThat(stringRedisTemplate.hasKey(userSessionKey(withdrawnUserId))).isFalse();
        assertThat(stringRedisTemplate.hasKey(refreshTokenKey(firstSession.sessionId()))).isFalse();
        assertThat(stringRedisTemplate.hasKey(refreshTokenKey(secondSession.sessionId()))).isFalse();

        assertThat(refreshTokenService.isSessionActive(activeUserId, otherUserSession.sessionId())).isTrue();
        assertThat(stringRedisTemplate.hasKey(userSessionKey(activeUserId))).isTrue();
        assertThat(stringRedisTemplate.hasKey(refreshTokenKey(otherUserSession.sessionId()))).isTrue();
    }

    @Test
    void rejectsReuseOfThePreviousRefreshTokenAfterRotation() {
        Long userId = 1L;
        JwtTokenPair currentTokenPair = tokenPair("session-a", "refresh-old");
        JwtTokenPair rotatedTokenPair = tokenPair("session-a", "refresh-new");
        JwtTokenPair replayedTokenPair = tokenPair("session-a", "refresh-replayed");

        refreshTokenService.saveRefreshToken(userId, currentTokenPair);
        refreshTokenService.rotateRefreshToken(userId, currentTokenPair.refreshToken(), rotatedTokenPair);

        assertThat(refreshTokenService.isSessionActive(userId, rotatedTokenPair.sessionId())).isTrue();
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(
                userId,
                currentTokenPair.refreshToken(),
                replayedTokenPair
        ))
                .isInstanceOfSatisfying(AuthException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void deletesOnlyTheCurrentSessionOnLogout() {
        Long userId = 1L;
        JwtTokenPair currentSession = tokenPair("session-a", "refresh-a");
        JwtTokenPair otherSession = tokenPair("session-b", "refresh-b");

        refreshTokenService.saveRefreshToken(userId, currentSession);
        refreshTokenService.saveRefreshToken(userId, otherSession);

        refreshTokenService.deleteSession(userId, currentSession.sessionId());

        assertThat(refreshTokenService.isSessionActive(userId, currentSession.sessionId())).isFalse();
        assertThat(refreshTokenService.isSessionActive(userId, otherSession.sessionId())).isTrue();
        assertThat(stringRedisTemplate.opsForZSet().range(userSessionKey(userId), 0, -1))
                .isEqualTo(Set.of(otherSession.sessionId()));
    }

    @Test
    void rejectsAnAccessTokenImmediatelyAfterItsSessionIsDeleted() throws Exception {
        Long userId = 1L;
        JwtTokenPair tokenPair = jwtTokenProvider.issueTokenPair(userId, UserRole.USER);
        refreshTokenService.saveRefreshToken(userId, tokenPair);

        AtomicBoolean validRequestContinued = new AtomicBoolean(false);
        MockHttpServletResponse validResponse = authenticate(
                tokenPair.accessToken(),
                (request, response) -> validRequestContinued.set(true)
        );

        assertThat(validRequestContinued).isTrue();
        assertThat(validResponse.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        SecurityContextHolder.clearContext();
        refreshTokenService.deleteSession(userId, tokenPair.sessionId());

        AtomicBoolean invalidRequestContinued = new AtomicBoolean(false);
        MockHttpServletResponse invalidResponse = authenticate(
                tokenPair.accessToken(),
                (request, response) -> invalidRequestContinued.set(true)
        );

        assertThat(invalidRequestContinued).isFalse();
        assertThat(invalidResponse.getStatus()).isEqualTo(401);
        assertThat(invalidResponse.getContentAsString()).contains(AuthErrorCode.INVALID_ACCESS_TOKEN.getReason().getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletResponse authenticate(String accessToken, FilterChain filterChain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        return response;
    }

    private JwtTokenPair tokenPair(String sessionId, String refreshToken) {
        return new JwtTokenPair(
                "access-%s".formatted(sessionId),
                refreshToken,
                sessionId,
                3600L
        );
    }

    private String refreshTokenKey(String sessionId) {
        return "%s:%s".formatted(REFRESH_TOKEN_KEY_PREFIX, sessionId);
    }

    private String userSessionKey(Long userId) {
        return "%s:%s".formatted(USER_SESSION_KEY_PREFIX, userId);
    }
}
