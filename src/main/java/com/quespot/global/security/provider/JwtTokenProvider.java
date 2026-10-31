package com.quespot.global.security.provider;

import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private enum JwtTokenType {
        ACCESS,
        REFRESH
    }

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ROLE_CLAIM = "role";
    private static final String SESSION_ID_CLAIM = "sid";

    private final SecretKey signingKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-minutes}") Long accessTokenExpirationMinutes,
            @Value("${app.jwt.refresh-token-expiration-days}") Long refreshTokenExpirationDays
    ) {
        validateExpiration(accessTokenExpirationMinutes, refreshTokenExpirationDays);
        this.signingKey = createSigningKey(secret);
        this.accessTokenExpiration = Duration.ofMinutes(accessTokenExpirationMinutes);
        this.refreshTokenExpiration = Duration.ofDays(refreshTokenExpirationDays);
    }

    public JwtTokenPair issueTokenPair(Long userId, UserRole role) {
        return issueTokenPair(userId, role, UUID.randomUUID().toString());
    }

    public JwtTokenPair issueTokenPair(Long userId, UserRole role, String sessionId) {
        Instant issuedAt = Instant.now();
        Instant accessTokenExpiresAt = issuedAt.plus(accessTokenExpiration);
        Instant refreshTokenExpiresAt = issuedAt.plus(refreshTokenExpiration);

        String accessToken = createToken(
                userId,
                role,
                sessionId,
                JwtTokenType.ACCESS,
                issuedAt,
                accessTokenExpiresAt
        );
        String refreshToken = createToken(
                userId,
                role,
                sessionId,
                JwtTokenType.REFRESH,
                issuedAt,
                refreshTokenExpiresAt
        );

        return new JwtTokenPair(
                accessToken,
                refreshToken,
                sessionId,
                refreshTokenExpiration.toSeconds()
        );
    }

    public AuthenticatedUser parseAccessToken(String token) {
        return parseToken(token, JwtTokenType.ACCESS, AuthErrorCode.INVALID_ACCESS_TOKEN);
    }

    public AuthenticatedUser parseRefreshToken(String token) {
        return parseToken(token, JwtTokenType.REFRESH, AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    private String createToken(
            Long userId,
            UserRole role,
            String sessionId,
            JwtTokenType tokenType,
            Instant issuedAt,
            Instant expiresAt
    ) {
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .claim(SESSION_ID_CLAIM, sessionId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));

        return builder
                .signWith(signingKey)
                .compact();
    }

    private AuthenticatedUser parseToken(
            String token,
            JwtTokenType expectedTokenType,
            AuthErrorCode errorCode
    ) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            String roleClaim = claims.get(ROLE_CLAIM, String.class);
            String tokenTypeClaim = claims.get(TOKEN_TYPE_CLAIM, String.class);
            String sessionId = claims.get(SESSION_ID_CLAIM, String.class);

            if (subject == null || roleClaim == null || tokenTypeClaim == null) {
                throw new AuthException(errorCode);
            }

            JwtTokenType tokenType = JwtTokenType.valueOf(tokenTypeClaim);
            if (tokenType != expectedTokenType) {
                throw new AuthException(errorCode);
            }

            Long userId = Long.valueOf(subject);
            UserRole role = UserRole.valueOf(roleClaim);

            if (expectedTokenType == JwtTokenType.REFRESH && (sessionId == null || sessionId.isBlank())) {
                throw new AuthException(errorCode);
            }

            return new AuthenticatedUser(userId, role, sessionId);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AuthException(errorCode);
        }
    }

    private SecretKey createSigningKey(String secret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64 value of at least 32 bytes.", exception);
        }
    }

    private void validateExpiration(Long accessTokenExpirationMinutes, Long refreshTokenExpirationDays) {
        if (accessTokenExpirationMinutes <= 0 || refreshTokenExpirationDays <= 0) {
            throw new IllegalStateException("JWT expiration values must be greater than zero.");
        }
    }
}
