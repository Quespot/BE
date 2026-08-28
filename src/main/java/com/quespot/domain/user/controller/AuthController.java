package com.quespot.domain.user.controller;

import com.quespot.domain.user.dto.req.SendEmailVerificationCodeRequestDTO;
import com.quespot.domain.user.dto.req.SignUpRequestDTO;
import com.quespot.domain.user.dto.req.VerifyEmailCodeRequestDTO;
import com.quespot.domain.user.dto.res.SendEmailVerificationCodeResponseDTO;
import com.quespot.domain.user.dto.res.SignUpResponseDTO;
import com.quespot.domain.user.dto.res.VerifyEmailCodeResponseDTO;
import com.quespot.domain.user.exception.code.AuthSuccessCode;
import com.quespot.domain.user.service.AuthService;
import com.quespot.domain.user.service.EmailVerificationService;
import com.quespot.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/sign-up")
    @Operation(
            summary = "회원가입",
            description = "이메일 인증이 완료된 사용자 계정을 생성합니다."
    )
    public ResponseEntity<ApiResponse<SignUpResponseDTO>> signUp(
            @Valid @RequestBody SignUpRequestDTO request
    ) {
        SignUpResponseDTO response = authService.signUp(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(AuthSuccessCode.SIGN_UP_SUCCESS, response));
    }

    private String resolveClientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/email/verification-code")
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

    @PostMapping("/email/verification")
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
