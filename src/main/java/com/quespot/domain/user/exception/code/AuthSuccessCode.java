package com.quespot.domain.user.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum AuthSuccessCode implements BaseCode {

    EMAIL_VERIFICATION_CODE_SENT(HttpStatus.OK,
            "AUTH_200_001",
            "이메일 인증 코드가 발송되었습니다."),

    EMAIL_VERIFICATION_COMPLETED(HttpStatus.OK,
            "AUTH_200_002",
            "이메일 인증이 완료되었습니다."),

    SIGN_UP_SUCCESS(HttpStatus.CREATED,
            "AUTH_201_001",
            "회원가입이 완료되었습니다."),

    LOGIN_SUCCESS(HttpStatus.OK,
            "AUTH_200_003",
            "로그인에 성공했습니다."),

    TOKEN_REISSUE_SUCCESS(HttpStatus.OK,
            "AUTH_200_004",
            "토큰이 재발급되었습니다."),

    LOGOUT_SUCCESS(HttpStatus.OK,
            "AUTH_200_005",
            "로그아웃되었습니다."),

    WITHDRAW_SUCCESS(HttpStatus.OK,
            "AUTH_200_006",
            "회원탈퇴가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AuthSuccessCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public SuccessReasonDTO getReason() {
        return new SuccessReasonDTO(null, true, code, message);
    }

    @Override
    public SuccessReasonDTO getReasonHttpStatus() {
        return new SuccessReasonDTO(httpStatus, true, code, message);
    }
}
