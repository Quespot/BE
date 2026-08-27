package com.quespot.global.apiPayload.code;

import org.springframework.http.HttpStatus;

public final class SuccessReasonDTO {

    private final HttpStatus httpStatus;
    private final Boolean isSuccess;
    private final String code;
    private final String message;

    public SuccessReasonDTO(HttpStatus httpStatus, Boolean isSuccess, String code, String message) {
        this.httpStatus = httpStatus;
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
