package com.quespot.domain.user.controller;

import com.quespot.domain.user.dto.res.LoginMethodListResponseDTO;
import com.quespot.domain.user.dto.res.OAuth2LinkStartResponseDTO;
import com.quespot.domain.user.exception.code.AuthSuccessCode;
import com.quespot.domain.user.service.LoginMethodService;
import com.quespot.domain.user.service.OAuth2LinkRequestService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.filter.OAuth2LinkRequestFilter;
import com.quespot.global.security.oauth2.OAuth2FrontendRedirectUriResolver;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/login-methods")
@Tag(name = "Auth", description = "인증 API")
public class LoginMethodController {

    private final LoginMethodService loginMethodService;
    private final OAuth2FrontendRedirectUriResolver frontendRedirectUriResolver;

    @GetMapping
    @Operation(summary = "연결 계정 목록 조회", description = "현재 사용자의 이메일 및 소셜 로그인 연결 상태를 조회합니다.")
    public ApiResponse<LoginMethodListResponseDTO> getLoginMethods(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.of(
                AuthSuccessCode.LOGIN_METHODS_FOUND,
                loginMethodService.getLoginMethods(authenticatedUser)
        );
    }

    @PostMapping("/{provider}")
    @Operation(
            summary = "소셜 계정 연결",
            description = "소셜 계정 연결을 위한 일회성 요청을 발급하고 OAuth 로그인 시작 URL을 반환합니다."
    )
    public ApiResponse<OAuth2LinkStartResponseDTO> startLink(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String provider,
            @Parameter(description = "OAuth 완료 후 돌아갈 프론트 콜백 주소. 서버 허용 목록에 등록된 주소만 사용할 수 있습니다.")
            @RequestParam(required = false) String frontendRedirectUri,
            HttpServletRequest httpServletRequest
    ) {
        String resolvedRedirectUri = frontendRedirectUriResolver.resolve(frontendRedirectUri);
        OAuth2LinkRequestService.IssuedLinkRequest issued =
                loginMethodService.startLink(authenticatedUser, provider);
        OAuth2LinkRequestFilter.storeLinkRequest(httpServletRequest, issued.nonce());
        String authorizationUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/auth/login/")
                .path(issued.provider().name().toLowerCase(Locale.ROOT))
                .queryParam("frontendRedirectUri", resolvedRedirectUri)
                .build()
                .encode()
                .toUriString();

        return ApiResponse.of(
                AuthSuccessCode.LOGIN_METHOD_LINK_STARTED,
                new OAuth2LinkStartResponseDTO(authorizationUrl, issued.expiresInSeconds())
        );
    }

    @DeleteMapping("/{provider}")
    @Operation(
            summary = "소셜 계정 연결 해제",
            description = "다른 로그인 수단이 남아 있는 경우 해당 소셜 계정의 서비스 내 연결을 해제합니다."
    )
    public ApiResponse<Void> unlink(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String provider
    ) {
        loginMethodService.unlink(authenticatedUser, provider);
        return ApiResponse.of(AuthSuccessCode.LOGIN_METHOD_UNLINKED, null);
    }
}
