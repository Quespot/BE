package com.quespot.domain.user.service;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class OAuth2LoginCodeService {

    private static final String LOGIN_CODE_KEY_PREFIX = "quespot:auth:oauth2-login-code";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> CONSUME_LOGIN_CODE_SCRIPT = new DefaultRedisScript<>("""
            local userId = redis.call('GET', KEYS[1])
            if userId then
                redis.call('DEL', KEYS[1])
            end
            return userId
            """, String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Duration expiration;

    public OAuth2LoginCodeService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.oauth2.login-code-expiration-seconds}") long expirationSeconds
    ) {
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("OAuth2 login code expiration must be greater than zero.");
        }

        this.stringRedisTemplate = stringRedisTemplate;
        this.expiration = Duration.ofSeconds(expirationSeconds);
    }

    // 일회용 로그인 코드 발급 로직
    public String issue(Long userId) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        stringRedisTemplate.opsForValue().set(loginCodeKey(code), String.valueOf(userId), expiration);
        return code;
    }

    // 일회용 로그인 코드 검증 및 소모 로직
    public Long consume(String code) {
        if (code == null || code.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LOGIN_CODE);
        }

        String userId = stringRedisTemplate.execute(
                CONSUME_LOGIN_CODE_SCRIPT,
                List.of(loginCodeKey(code.trim()))
        );
        if (userId == null) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LOGIN_CODE);
        }

        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LOGIN_CODE);
        }
    }

    private String loginCodeKey(String code) {
        return "%s:%s".formatted(LOGIN_CODE_KEY_PREFIX, code);
    }
}
