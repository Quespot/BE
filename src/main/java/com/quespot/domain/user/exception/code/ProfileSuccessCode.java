package com.quespot.domain.user.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum ProfileSuccessCode implements BaseCode {

    PROFILE_CREATED(HttpStatus.CREATED,
            "PROFILE_201_001",
            "프로필이 생성되었습니다."),

    PROFILE_FOUND(HttpStatus.OK,
            "PROFILE_200_001",
            "프로필을 조회했습니다."),

    PROFILE_UPDATED(HttpStatus.OK,
            "PROFILE_200_002",
            "프로필이 수정되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ProfileSuccessCode(HttpStatus httpStatus, String code, String message) {
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
