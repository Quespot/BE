package com.quespot.global.security.filter;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.service.OAuth2LinkRequestService;
import com.quespot.global.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LinkRequestFilter extends OncePerRequestFilter {

    public static final String LINK_REQUEST_SESSION_ATTRIBUTE =
            OAuth2LinkRequestFilter.class.getName() + ".LINK_REQUEST";
    private static final String AUTHORIZATION_BASE_URI = "/api/auth/login/";

    private final OAuth2LinkRequestService oAuth2LinkRequestService;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolveProvider(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String linkNonce = getLinkRequest(request);
        if (linkNonce == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            oAuth2LinkRequestService.validate(linkNonce, resolveProvider(request));
            filterChain.doFilter(request, response);
        } catch (AuthException exception) {
            clearLinkRequest(request);
            securityErrorResponseWriter.write(response, exception.getErrorCode());
        }
    }

    public static void storeLinkRequest(HttpServletRequest request, String nonce) {
        request.getSession(true).setAttribute(LINK_REQUEST_SESSION_ATTRIBUTE, nonce);
    }

    public static String takeLinkRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(LINK_REQUEST_SESSION_ATTRIBUTE);
        session.removeAttribute(LINK_REQUEST_SESSION_ATTRIBUTE);
        return value instanceof String token ? token : null;
    }

    public static void clearLinkRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(LINK_REQUEST_SESSION_ATTRIBUTE);
        }
    }

    private static String getLinkRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(LINK_REQUEST_SESSION_ATTRIBUTE);
        return value instanceof String nonce ? nonce : null;
    }

    private String resolveProvider(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = request.getServletPath();
        if (!path.startsWith(AUTHORIZATION_BASE_URI)) {
            return null;
        }

        String provider = path.substring(AUTHORIZATION_BASE_URI.length());
        return provider.isBlank() || provider.contains("/") ? null : provider;
    }
}
