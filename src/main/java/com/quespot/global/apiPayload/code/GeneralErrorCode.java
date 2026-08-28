package com.quespot.global.apiPayload.code;

import org.springframework.http.HttpStatus;

public enum GeneralErrorCode implements BaseErrorCode {

    COMMON_400_001(HttpStatus.BAD_REQUEST,
            "COMMON_400_001",
            "잘못된 요청입니다."),

    COMMON_400_002(HttpStatus.BAD_REQUEST,
            "COMMON_400_002",
            "입력값 검증에 실패했습니다."),

    COMMON_404_001(HttpStatus.NOT_FOUND,
            "COMMON_404_001",
            "요청한 리소스를 찾을 수 없습니다."),

    COMMON_405_001(HttpStatus.METHOD_NOT_ALLOWED,
            "COMMON_405_001",
            "지원하지 않는 HTTP 메서드입니다."),

    COMMON_500_001(HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON_500_001",
            "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GeneralErrorCode(HttpStatus httpStatus, String code, String message) {
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
