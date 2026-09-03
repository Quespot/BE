package com.quespot.domain.user.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements BaseErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "AUTH_401_001",
            "인증이 필요합니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN,
            "AUTH_403_001",
            "접근 권한이 없습니다."),

    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST,
            "AUTH_400_001",
            "이메일 인증이 완료되지 않았습니다."),

    PASSWORD_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST,
            "AUTH_400_003",
            "비밀번호와 비밀번호 확인이 일치하지 않습니다."),

    INVALID_EMAIL_VERIFICATION_CODE(HttpStatus.BAD_REQUEST,
            "AUTH_400_002",
            "이메일 인증 코드가 올바르지 않거나 만료되었습니다."),

    EMAIL_VERIFICATION_REQUEST_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
            "AUTH_429_001",
            "이메일 인증 코드 발송 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
            "AUTH_429_002",
            "이메일 인증 코드 입력 횟수를 초과했습니다. 인증 코드를 다시 발급해주세요."),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT,
            "AUTH_409_001",
            "이미 가입된 이메일입니다."),

    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED,
            "AUTH_401_002",
            "이메일 또는 비밀번호가 올바르지 않습니다."),

    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH_401_003",
            "Access Token이 올바르지 않거나 만료되었습니다."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH_401_004",
            "Refresh Token이 올바르지 않거나 만료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public ErrorReasonDTO getReason() {
        return new ErrorReasonDTO(null, false, code, message);
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return new ErrorReasonDTO(httpStatus, false, code, message);
    }
}
