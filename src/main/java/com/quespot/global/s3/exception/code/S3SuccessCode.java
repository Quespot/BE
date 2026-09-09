package com.quespot.global.s3.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum S3SuccessCode implements BaseCode {

    PRESIGNED_UPLOAD_URL_CREATED(HttpStatus.OK,
            "S3_200_001",
            "파일 업로드 URL을 발급했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    S3SuccessCode(HttpStatus httpStatus, String code, String message) {
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
