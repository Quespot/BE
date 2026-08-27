package com.quespot.global.apiPayload.exception;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;

public class GeneralException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public GeneralException(BaseErrorCode errorCode) {
        super(errorCode.getReason().getMessage());
        this.errorCode = errorCode;
    }

    public BaseErrorCode getErrorCode() {
        return errorCode;
    }

    public ErrorReasonDTO getErrorReason() {
        return errorCode.getReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus() {
        return errorCode.getReasonHttpStatus();
    }
}
