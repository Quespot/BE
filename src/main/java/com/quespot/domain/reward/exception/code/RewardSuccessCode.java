package com.quespot.domain.reward.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum RewardSuccessCode implements BaseCode {

    POINTS_FOUND(HttpStatus.OK,
            "REWARD_200_001",
            "보유 포인트를 조회했습니다."),

    REWARD_ACTIVITIES_FOUND(HttpStatus.OK,
            "REWARD_200_002",
            "보상 활동 내역을 조회했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    RewardSuccessCode(HttpStatus httpStatus, String code, String message) {
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
