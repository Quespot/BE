package com.quespot.global.security.handler;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.service.OAuth2LoginService;
import com.quespot.domain.user.service.OAuth2ProviderToken;
import com.quespot.global.apiPayload.code.GeneralErrorCode;
import com.quespot.global.security.filter.OAuth2LinkRequestFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2LoginService oAuth2LoginService;
    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider;
    private final String frontendRedirectUri;

    public OAuth2LoginSuccessHandler(
            OAuth2LoginService oAuth2LoginService,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider,
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri
    ) {
        this.oAuth2LoginService = oAuth2LoginService;
        this.authorizedClientServiceProvider = authorizedClientServiceProvider;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken token)) {
                redirectWithError(response, AuthErrorCode.OAUTH2_LOGIN_FAILED.getReason().getCode());
                return;
            }

            OAuth2User oAuth2User = token.getPrincipal();
            OAuth2ProviderToken providerToken = resolveProviderToken(token);
            String linkNonce = OAuth2LinkRequestFilter.takeLinkRequest(request);
            if (linkNonce != null) {
                LoginProvider linkedProvider = oAuth2LoginService.linkAccount(
                        linkNonce,
                        token.getAuthorizedClientRegistrationId(),
                        oAuth2User,
                        providerToken
                );
                String redirectUri = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                        .queryParam("linkedProvider", linkedProvider.name())
                        .build()
                        .encode()
                        .toUriString();
                response.sendRedirect(redirectUri);
                return;
            }

            String loginCode = oAuth2LoginService.prepareLogin(
                    token.getAuthorizedClientRegistrationId(),
                    oAuth2User,
                    providerToken
            );
            String redirectUri = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("code", loginCode)
                    .build()
                    .encode()
                    .toUriString();
            response.sendRedirect(redirectUri);
        } catch (AuthException exception) {
            redirectWithError(response, exception.getErrorReason().getCode());
        } catch (DataAccessException exception) {
            redirectWithError(response, GeneralErrorCode.COMMON_503_001.getReason().getCode());
        } finally {
            OAuth2LinkRequestFilter.clearLinkRequest(request);
            SecurityContextHolder.clearContext();
        }
    }

    private OAuth2ProviderToken resolveProviderToken(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClientService authorizedClientService =
                authorizedClientServiceProvider.getIfAvailable();
        if (authorizedClientService == null) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }

        String refreshToken = authorizedClient.getRefreshToken() == null
                ? null
                : authorizedClient.getRefreshToken().getTokenValue();
        LocalDateTime expiresAt = authorizedClient.getAccessToken().getExpiresAt() == null
                ? null
                : LocalDateTime.ofInstant(
                        authorizedClient.getAccessToken().getExpiresAt(),
                        ZoneOffset.UTC
                );
        return new OAuth2ProviderToken(
                authorizedClient.getAccessToken().getTokenValue(),
                refreshToken,
                expiresAt
        );
    }

    private void redirectWithError(HttpServletResponse response, String errorCode) throws IOException {
        String redirectUri = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", errorCode)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUri);
    }
}
