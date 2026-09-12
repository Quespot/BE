package com.quespot.domain.notification.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements BaseErrorCode {

    FCM_TOKEN_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "NOTIFICATION_500_001",
            "FCM 토큰 등록에 실패했습니다."),

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "NOTIFICATION_404_001",
            "알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    NotificationErrorCode(HttpStatus httpStatus, String code, String message) {
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
