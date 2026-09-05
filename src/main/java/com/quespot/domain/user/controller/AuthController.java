package com.quespot.domain.user.controller;

import com.quespot.domain.user.dto.req.LoginRequestDTO;
import com.quespot.domain.user.dto.req.OAuth2LoginCodeExchangeRequestDTO;
import com.quespot.domain.user.dto.req.SendEmailVerificationCodeRequestDTO;
import com.quespot.domain.user.dto.req.SignUpRequestDTO;
import com.quespot.domain.user.dto.req.VerifyEmailCodeRequestDTO;
import com.quespot.domain.user.dto.res.LoginResponseDTO;
import com.quespot.domain.user.dto.res.SendEmailVerificationCodeResponseDTO;
import com.quespot.domain.user.dto.res.SignUpResponseDTO;
import com.quespot.domain.user.dto.res.TokenReissueResponseDTO;
import com.quespot.domain.user.dto.res.VerifyEmailCodeResponseDTO;
import com.quespot.domain.user.dto.token.LoginResultDTO;
import com.quespot.domain.user.dto.token.TokenReissueResultDTO;
import com.quespot.domain.user.exception.code.AuthSuccessCode;
import com.quespot.domain.user.service.AuthService;
import com.quespot.domain.user.service.EmailVerificationService;
import com.quespot.domain.user.service.OAuth2LoginService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import com.quespot.global.security.provider.RefreshTokenCookieProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final OAuth2LoginService oAuth2LoginService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @PostMapping("/sign-up")
    @SecurityRequirements
    @Operation(
            summary = "회원가입",
            description = "이메일 인증과 비밀번호 확인이 완료된 사용자 계정을 생성합니다. 프로필은 로그인 후 별도로 생성합니다."
    )
    public ResponseEntity<ApiResponse<SignUpResponseDTO>> signUp(
            @Valid @RequestBody SignUpRequestDTO request
    ) {
        SignUpResponseDTO response = authService.signUp(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(AuthSuccessCode.SIGN_UP_SUCCESS, response));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "일반 로그인",
            description = "이메일과 비밀번호를 검증하고 Access Token을 응답하며, Refresh Token은 쿠키로 발급합니다."
    )
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        LoginResultDTO result = authService.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieProvider.create(
                                result.refreshToken(),
                                result.refreshTokenExpiresInSeconds()
                        ).toString()
                )
                .body(ApiResponse.of(AuthSuccessCode.LOGIN_SUCCESS, result.response()));
    }

    @PostMapping("/login/oauth2/exchange")
    @SecurityRequirements
    @Operation(
            summary = "소셜 로그인 코드 교환",
            description = "OAuth2 로그인 완료 후 발급된 일회용 코드를 Quespot Access Token과 Refresh Token으로 교환합니다."
    )
    public ResponseEntity<ApiResponse<LoginResponseDTO>> exchangeOAuth2LoginCode(
            @Valid @RequestBody OAuth2LoginCodeExchangeRequestDTO request
    ) {
        LoginResultDTO result = oAuth2LoginService.exchangeLoginCode(request.code());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieProvider.create(
                                result.refreshToken(),
                                result.refreshTokenExpiresInSeconds()
                        ).toString()
                )
                .body(ApiResponse.of(AuthSuccessCode.LOGIN_SUCCESS, result.response()));
    }

    @PostMapping("/reissue")
    @SecurityRequirements
    @Operation(
            summary = "토큰 재발급",
            description = "쿠키의 Refresh Token을 검증하고 새로운 Access Token을 응답하며, Refresh Token 쿠키를 갱신합니다."
    )
    public ResponseEntity<ApiResponse<TokenReissueResponseDTO>> reissueToken(
            @Parameter(hidden = true)
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false)
            String refreshToken
    ) {
        TokenReissueResultDTO result = authService.reissueToken(refreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieProvider.create(
                                result.refreshToken(),
                                result.refreshTokenExpiresInSeconds()
                        ).toString()
                )
                .body(ApiResponse.of(AuthSuccessCode.TOKEN_REISSUE_SUCCESS, result.response()));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 Access Token의 세션을 삭제하고 Refresh Token 쿠키를 만료합니다."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        authService.logout(authenticatedUser);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.delete().toString())
                .body(ApiResponse.<Void>of(AuthSuccessCode.LOGOUT_SUCCESS, null));
    }

    @DeleteMapping("/withdraw")
    @Operation(
            summary = "회원탈퇴",
            description = "계정을 탈퇴 상태로 변경하고 이메일을 마스킹하며 모든 로그인 세션을 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        authService.withdraw(authenticatedUser);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.delete().toString())
                .body(ApiResponse.<Void>of(AuthSuccessCode.WITHDRAW_SUCCESS, null));
    }

    private String resolveClientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/email/verification-request")
    @SecurityRequirements
    @Operation(
            summary = "이메일 인증 코드 발송",
            description = "회원가입에 사용할 이메일 인증 코드를 발송합니다."
    )
    public ApiResponse<SendEmailVerificationCodeResponseDTO> sendEmailVerificationCode(
            @Valid @RequestBody SendEmailVerificationCodeRequestDTO request,
            HttpServletRequest httpServletRequest
    ) {
        SendEmailVerificationCodeResponseDTO response = emailVerificationService.sendVerificationCode(
                request,
                resolveClientKey(httpServletRequest)
        );

        return ApiResponse.of(AuthSuccessCode.EMAIL_VERIFICATION_CODE_SENT, response);
    }

    @PostMapping("/email/verification-confirm")
    @SecurityRequirements
    @Operation(
            summary = "이메일 인증 코드 검증",
            description = "회원가입에 사용할 이메일 인증 코드를 검증합니다."
    )
    public ApiResponse<VerifyEmailCodeResponseDTO> verifyEmailCode(
            @Valid @RequestBody VerifyEmailCodeRequestDTO request,
            HttpServletRequest httpServletRequest
    ) {
        VerifyEmailCodeResponseDTO response = emailVerificationService.verifyEmailCode(
                request,
                resolveClientKey(httpServletRequest)
        );

        return ApiResponse.of(AuthSuccessCode.EMAIL_VERIFICATION_COMPLETED, response);
    }
}
