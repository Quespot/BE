package com.quespot.global.apiPayload.code;

import org.springframework.http.HttpStatus;

public enum GeneralSuccessCode implements BaseCode {

    COMMON_200(HttpStatus.OK,
            "COMMON_200",
            "요청에 성공했습니다."),

    COMMON_201(HttpStatus.CREATED,
            "COMMON_201",
            "요청에 성공하여 리소스가 생성되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GeneralSuccessCode(HttpStatus httpStatus, String code, String message) {
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
