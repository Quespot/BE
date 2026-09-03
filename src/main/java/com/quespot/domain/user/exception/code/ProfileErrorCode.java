package com.quespot.domain.user.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum ProfileErrorCode implements BaseErrorCode {

    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "PROFILE_404_001",
            "프로필이 생성되지 않았습니다."),

    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT,
            "PROFILE_409_001",
            "이미 프로필이 생성되어 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ProfileErrorCode(HttpStatus httpStatus, String code, String message) {
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
