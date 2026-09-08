package com.quespot.domain.tour.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum TourSyncSuccessCode implements BaseCode {

    SYNC_COMPLETED(HttpStatus.OK,
            "TOUR_200_001",
            "TourAPI 동기화를 완료했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    TourSyncSuccessCode(HttpStatus httpStatus, String code, String message) {
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
