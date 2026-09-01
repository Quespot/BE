package com.quespot.domain.item.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum ItemErrorCode implements BaseErrorCode {

    ITEM_NOT_OWNED(HttpStatus.NOT_FOUND,
            "ITEM_404_001",
            "보유하지 않은 아이템입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ItemErrorCode(HttpStatus httpStatus, String code, String message) {
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
