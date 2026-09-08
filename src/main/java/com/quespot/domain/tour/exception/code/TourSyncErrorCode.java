package com.quespot.domain.tour.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum TourSyncErrorCode implements BaseErrorCode {

    SYNC_ALREADY_RUNNING(HttpStatus.CONFLICT,
            "TOUR_409_001",
            "TourAPI 동기화가 이미 실행 중입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    TourSyncErrorCode(HttpStatus httpStatus, String code, String message) {
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
