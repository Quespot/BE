package com.quespot.global.s3.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum S3ErrorCode implements BaseErrorCode {

    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST,
            "S3_400_001",
            "파일명이 올바르지 않습니다."),

    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST,
            "S3_400_002",
            "지원하지 않는 이미지 형식입니다."),

    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST,
            "S3_400_003",
            "파일 크기가 올바르지 않습니다."),

    INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST,
            "S3_400_004",
            "S3 객체 키가 올바르지 않습니다."),

    S3_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE,
            "S3_503_001",
            "S3 저장소가 설정되지 않았습니다."),

    PRESIGNED_URL_GENERATION_FAILED(HttpStatus.BAD_GATEWAY,
            "S3_502_001",
            "파일 업로드 URL 생성에 실패했습니다."),

    OBJECT_DELETE_FAILED(HttpStatus.BAD_GATEWAY,
            "S3_502_002",
            "파일 삭제에 실패했습니다."),

    PRESIGNED_DOWNLOAD_URL_GENERATION_FAILED(HttpStatus.BAD_GATEWAY,
            "S3_502_003",
            "파일 조회 URL 생성에 실패했습니다."),

    OBJECT_KEY_WRONG_PURPOSE(HttpStatus.BAD_REQUEST,
            "S3_400_006",
            "잘못된 업로드 경로의 파일입니다."),

    OBJECT_KEY_OWNER_MISMATCH(HttpStatus.BAD_REQUEST,
            "S3_400_007",
            "본인이 업로드한 파일만 등록할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    S3ErrorCode(HttpStatus httpStatus, String code, String message) {
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
