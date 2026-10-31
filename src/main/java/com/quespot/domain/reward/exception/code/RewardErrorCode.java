package com.quespot.domain.reward.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum RewardErrorCode implements BaseErrorCode {
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST,
            "REWARD_400_001",
            "포인트가 부족합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    RewardErrorCode(HttpStatus httpStatus, String code, String message) {
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
