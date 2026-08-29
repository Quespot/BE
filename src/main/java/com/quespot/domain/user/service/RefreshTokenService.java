package com.quespot.domain.user.service;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.global.security.provider.JwtTokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "quespot:auth:refresh";
    private static final String USER_SESSION_KEY_PREFIX = "quespot:auth:user-sessions";
    private static final Long TOKEN_ROTATION_SUCCEEDED = 1L;
    private static final DefaultRedisScript<Long> SAVE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'tokenHash', ARGV[2])
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))

            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', tonumber(ARGV[4]))
            redis.call('ZADD', KEYS[2], tonumber(ARGV[5]), ARGV[6])
            redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]))

            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local savedUserId = redis.call('HGET', KEYS[1], 'userId')
            local savedTokenHash = redis.call('HGET', KEYS[1], 'tokenHash')

            if not savedUserId or not savedTokenHash then
                return 0
            end

            if savedUserId ~= ARGV[1] or savedTokenHash ~= ARGV[2] then
                return 0
            end

            redis.call('HSET', KEYS[1], 'tokenHash', ARGV[3])
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))

            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', tonumber(ARGV[5]))
            redis.call('ZADD', KEYS[2], tonumber(ARGV[6]), ARGV[7])
            redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4]))

            return 1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public void saveRefreshToken(Long userId, JwtTokenPair tokenPair) {
        long currentTimeMillis = Instant.now().toEpochMilli();
        long expirationTimeMillis = currentTimeMillis + tokenPair.refreshTokenExpiresInSeconds() * 1000;

        stringRedisTemplate.execute(
                SAVE_REFRESH_TOKEN_SCRIPT,
                List.of(
                        refreshTokenKey(tokenPair.sessionId()),
                        userSessionKey(userId)
                ),
                String.valueOf(userId),
                hashToken(tokenPair.refreshToken()),
                String.valueOf(tokenPair.refreshTokenExpiresInSeconds()),
                String.valueOf(currentTimeMillis),
                String.valueOf(expirationTimeMillis),
                tokenPair.sessionId()
        );
    }

    public void rotateRefreshToken(
            Long userId,
            String currentRefreshToken,
            JwtTokenPair newTokenPair
    ) {
        long currentTimeMillis = Instant.now().toEpochMilli();
        long expirationTimeMillis = currentTimeMillis + newTokenPair.refreshTokenExpiresInSeconds() * 1000;
        Long result = stringRedisTemplate.execute(
                ROTATE_REFRESH_TOKEN_SCRIPT,
                List.of(
                        refreshTokenKey(newTokenPair.sessionId()),
                        userSessionKey(userId)
                ),
                String.valueOf(userId),
                hashToken(currentRefreshToken),
                hashToken(newTokenPair.refreshToken()),
                String.valueOf(newTokenPair.refreshTokenExpiresInSeconds()),
                String.valueOf(currentTimeMillis),
                String.valueOf(expirationTimeMillis),
                newTokenPair.sessionId()
        );

        if (!TOKEN_ROTATION_SUCCEEDED.equals(result)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }

    private String refreshTokenKey(String sessionId) {
        return "%s:%s".formatted(REFRESH_TOKEN_KEY_PREFIX, sessionId);
    }

    private String userSessionKey(Long userId) {
        return "%s:%s".formatted(USER_SESSION_KEY_PREFIX, userId);
    }
}
