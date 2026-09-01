package com.quespot.domain.notification.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum NotificationSuccessCode implements BaseCode {

    FCM_TOKEN_REGISTERED(HttpStatus.OK,
            "NOTIFICATION_200_001",
            "FCM 토큰이 등록되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    NotificationSuccessCode(HttpStatus httpStatus, String code, String message) {
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
