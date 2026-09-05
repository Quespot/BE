package com.quespot.global.security.handler;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.service.OAuth2LoginService;
import com.quespot.global.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2LoginService oAuth2LoginService;
    private final String frontendRedirectUri;

    public OAuth2LoginSuccessHandler(
            OAuth2LoginService oAuth2LoginService,
            @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri
    ) {
        this.oAuth2LoginService = oAuth2LoginService;
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
            String loginCode = oAuth2LoginService.prepareLogin(
                    token.getAuthorizedClientRegistrationId(),
                    oAuth2User
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
            SecurityContextHolder.clearContext();
        }
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
