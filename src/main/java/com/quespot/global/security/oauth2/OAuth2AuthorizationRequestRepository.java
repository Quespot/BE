package com.quespot.global.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String REDIRECTS_SESSION_ATTRIBUTE =
            OAuth2AuthorizationRequestRepository.class.getName() + ".REDIRECTS";

    private final OAuth2FrontendRedirectUriResolver redirectUriResolver;
    private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        delegate.saveAuthorizationRequest(authorizationRequest, request, response);
        if (authorizationRequest == null || authorizationRequest.getState() == null) {
            return;
        }

        String redirectUri = resolveRequestRedirectUri(request);
        HttpSession session = request.getSession(true);
        synchronized (session) {
            redirects(session, true).put(authorizationRequest.getState(), redirectUri);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return delegate.removeAuthorizationRequest(request, response);
    }

    public String takeFrontendRedirectUri(HttpServletRequest request) {
        String state = request.getParameter("state");
        HttpSession session = request.getSession(false);
        if (state != null && session != null) {
            synchronized (session) {
                Map<String, String> redirects = redirects(session, false);
                if (redirects != null) {
                    String redirectUri = redirects.remove(state);
                    if (redirects.isEmpty()) {
                        session.removeAttribute(REDIRECTS_SESSION_ATTRIBUTE);
                    }
                    if (redirectUri != null) {
                        return redirectUri;
                    }
                }
            }
        }

        Object requestValue = request.getAttribute(OAuth2FrontendRedirectUriResolver.REQUEST_ATTRIBUTE);
        return requestValue instanceof String redirectUri
                ? redirectUri
                : redirectUriResolver.getDefaultRedirectUri();
    }

    private String resolveRequestRedirectUri(HttpServletRequest request) {
        Object requestValue = request.getAttribute(OAuth2FrontendRedirectUriResolver.REQUEST_ATTRIBUTE);
        if (requestValue instanceof String redirectUri) {
            return redirectUri;
        }
        return redirectUriResolver.resolve(request.getParameter("frontendRedirectUri"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> redirects(HttpSession session, boolean create) {
        Object value = session.getAttribute(REDIRECTS_SESSION_ATTRIBUTE);
        if (value instanceof Map<?, ?>) {
            return (Map<String, String>) value;
        }
        if (!create) {
            return null;
        }

        Map<String, String> redirects = new HashMap<>();
        session.setAttribute(REDIRECTS_SESSION_ATTRIBUTE, redirects);
        return redirects;
    }
}
