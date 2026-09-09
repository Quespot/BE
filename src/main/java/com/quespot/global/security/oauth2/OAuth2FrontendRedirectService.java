package com.quespot.global.security.oauth2;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OAuth2FrontendRedirectService {

    public static final String REQUEST_ATTRIBUTE =
            OAuth2FrontendRedirectService.class.getName() + ".REDIRECT_URI";

    private final String defaultRedirectUri;
    private final Set<String> allowedRedirectUris;

    public OAuth2FrontendRedirectService(
            @Value("${app.oauth2.frontend-redirect-uri}") String defaultRedirectUri,
            @Value("${app.oauth2.allowed-frontend-redirect-uris}") String allowedRedirectUris
    ) {
        this.defaultRedirectUri = validateConfiguredUri(defaultRedirectUri);
        this.allowedRedirectUris = Arrays.stream(allowedRedirectUris.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::validateConfiguredUri)
                .collect(Collectors.toUnmodifiableSet());

        if (!this.allowedRedirectUris.contains(this.defaultRedirectUri)) {
            throw new IllegalStateException("The default OAuth2 frontend redirect URI must be allowed.");
        }
    }

    public String resolve(String requestedRedirectUri) {
        if (requestedRedirectUri == null || requestedRedirectUri.isBlank()) {
            return defaultRedirectUri;
        }

        String normalized = requestedRedirectUri.trim();
        if (!allowedRedirectUris.contains(normalized)) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH2_FRONTEND_REDIRECT_URI);
        }
        return normalized;
    }

    public String getDefaultRedirectUri() {
        return defaultRedirectUri;
    }

    private String validateConfiguredUri(String value) {
        try {
            String normalized = value.trim();
            URI uri = URI.create(normalized);
            boolean supportedScheme = "https".equalsIgnoreCase(uri.getScheme())
                    || ("http".equalsIgnoreCase(uri.getScheme())
                        && isLoopbackHost(uri.getHost()));
            if (!uri.isAbsolute()
                    || !supportedScheme
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("OAuth2 frontend redirect URI configuration is invalid.");
        }
    }

    private boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }

        String normalized = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        return "localhost".equalsIgnoreCase(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized);
    }
}
