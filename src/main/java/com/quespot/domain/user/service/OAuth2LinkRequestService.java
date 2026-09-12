package com.quespot.domain.user.service;

import com.quespot.domain.user.enums.LoginProvider;
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
import java.util.Locale;

@Service
public class OAuth2LinkRequestService {

    private static final String LINK_REQUEST_KEY_PREFIX = "quespot:auth:oauth2-link-nonce";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> CONSUME_LINK_REQUEST_SCRIPT = new DefaultRedisScript<>("""
            local request = redis.call('GET', KEYS[1])
            if request then
                redis.call('DEL', KEYS[1])
            end
            return request
            """, String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Duration expiration;
    private final boolean googleEnabled;
    private final boolean kakaoEnabled;
    private final boolean naverEnabled;

    public OAuth2LinkRequestService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.oauth2.link-request-expiration-seconds}") long expirationSeconds,
            @Value("${app.oauth2.google.enabled:false}") boolean googleEnabled,
            @Value("${app.oauth2.kakao.enabled:false}") boolean kakaoEnabled,
            @Value("${app.oauth2.naver.enabled:false}") boolean naverEnabled
    ) {
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("OAuth2 link request expiration must be greater than zero.");
        }

        this.stringRedisTemplate = stringRedisTemplate;
        this.expiration = Duration.ofSeconds(expirationSeconds);
        this.googleEnabled = googleEnabled;
        this.kakaoEnabled = kakaoEnabled;
        this.naverEnabled = naverEnabled;
    }

    // 소셜 계정 연결용 일회성 요청 발급 로직
    public IssuedLinkRequest issue(Long userId, String providerValue) {
        LoginProvider provider = resolveSocialProvider(providerValue);
        validateEnabled(provider);

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String value = "%d|%s".formatted(userId, provider.name());
        stringRedisTemplate.opsForValue().set(linkRequestKey(nonce), value, expiration);

        return new IssuedLinkRequest(provider, nonce, expiration.toSeconds());
    }

    // OAuth 로그인 시작 전에 연결 요청과 제공자를 검증하는 로직
    public void validate(String nonce, String providerValue) {
        LoginProvider provider = resolveSocialProvider(providerValue);
        LinkRequest linkRequest = parse(read(nonce));
        if (linkRequest.provider() != provider) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LINK_REQUEST);
        }
    }

    // OAuth 콜백에서 연결 요청을 한 번만 소모하는 로직
    public LinkRequest consume(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LINK_REQUEST);
        }

        String value = stringRedisTemplate.execute(
                CONSUME_LINK_REQUEST_SCRIPT,
                List.of(linkRequestKey(nonce.trim()))
        );
        return parse(value);
    }

    // 소셜 로그인 제공자 확인 로직
    public LoginProvider resolveSocialProvider(String providerValue) {
        if (providerValue == null || providerValue.isBlank()) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_LOGIN_PROVIDER);
        }

        try {
            LoginProvider provider = LoginProvider.valueOf(providerValue.toUpperCase(Locale.ROOT));
            if (provider == LoginProvider.EMAIL) {
                throw new IllegalArgumentException();
            }
            return provider;
        } catch (IllegalArgumentException exception) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_LOGIN_PROVIDER);
        }
    }

    private void validateEnabled(LoginProvider provider) {
        boolean enabled = switch (provider) {
            case GOOGLE -> googleEnabled;
            case KAKAO -> kakaoEnabled;
            case NAVER -> naverEnabled;
            default -> false;
        };
        if (!enabled) {
            throw new AuthException(AuthErrorCode.DISABLED_LOGIN_PROVIDER);
        }
    }

    private String read(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LINK_REQUEST);
        }

        String value = stringRedisTemplate.opsForValue().get(linkRequestKey(nonce.trim()));
        if (value == null) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LINK_REQUEST);
        }
        return value;
    }

    private LinkRequest parse(String value) {
        if (value == null) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LINK_REQUEST);
        }

        String[] values = value.split("\\|", -1);
        if (values.length != 2) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LINK_REQUEST);
        }

        try {
            return new LinkRequest(Long.valueOf(values[0]), LoginProvider.valueOf(values[1]));
        } catch (IllegalArgumentException exception) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_LINK_REQUEST);
        }
    }

    private String linkRequestKey(String nonce) {
        return "%s:%s".formatted(LINK_REQUEST_KEY_PREFIX, nonce);
    }

    public record IssuedLinkRequest(LoginProvider provider, String nonce, long expiresInSeconds) {
    }

    public record LinkRequest(Long userId, LoginProvider provider) {
    }
}
