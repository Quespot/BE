package com.quespot.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.service.RefreshTokenService;
import com.quespot.global.apiPayload.code.GeneralErrorCode;
import com.quespot.global.security.handler.SecurityErrorResponseWriter;
import com.quespot.global.security.provider.JwtTokenPair;
import com.quespot.global.security.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET = Base64.getEncoder().encodeToString(
            "test-jwt-secret-key-must-be-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsServiceUnavailableWhenRedisSessionValidationFails() throws Exception {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(JWT_SECRET, 30L, 14L);
        JwtTokenPair tokenPair = jwtTokenProvider.issueTokenPair(1L, UserRole.USER);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        when(refreshTokenService.isSessionActive(1L, tokenPair.sessionId()))
                .thenThrow(new DataAccessResourceFailureException("Redis unavailable"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                refreshTokenService,
                new SecurityErrorResponseWriter(new ObjectMapper())
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenPair.accessToken());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filterChainContinued = new AtomicBoolean(false);
        FilterChain filterChain = (servletRequest, servletResponse) -> filterChainContinued.set(true);

        filter.doFilter(request, response, filterChain);

        assertThat(filterChainContinued).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains(GeneralErrorCode.COMMON_503_001.getReason().getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
