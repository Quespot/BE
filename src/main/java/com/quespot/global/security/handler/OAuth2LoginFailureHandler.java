package com.quespot.global.security.handler;

import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.global.security.filter.OAuth2LinkRequestFilter;
import com.quespot.global.security.oauth2.OAuth2AuthorizationRequestRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuth2AuthorizationRequestRepository authorizationRequestRepository;

    public OAuth2LoginFailureHandler(
            OAuth2AuthorizationRequestRepository authorizationRequestRepository
    ) {
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        OAuth2LinkRequestFilter.clearLinkRequest(request);
        String frontendRedirectUri = authorizationRequestRepository.takeFrontendRedirectUri(request);
        String redirectUri = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", AuthErrorCode.OAUTH2_LOGIN_FAILED.getReason().getCode())
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUri);
    }
}
