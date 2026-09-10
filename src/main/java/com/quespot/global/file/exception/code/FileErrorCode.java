package com.quespot.global.file.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum FileErrorCode implements BaseErrorCode {

    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST,
            "FILE_400_001",
            "파일명이 올바르지 않습니다."),

    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST,
            "FILE_400_002",
            "지원하지 않는 이미지 형식입니다."),

    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST,
            "FILE_400_003",
            "파일 크기가 올바르지 않습니다."),

    INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST,
            "FILE_400_004",
            "파일 객체 키가 올바르지 않습니다."),

    FILE_STORAGE_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE,
            "FILE_503_001",
            "파일 저장소가 설정되지 않았습니다."),

    PRESIGNED_URL_GENERATION_FAILED(HttpStatus.BAD_GATEWAY,
            "FILE_502_001",
            "파일 업로드 URL 생성에 실패했습니다."),

    OBJECT_DELETE_FAILED(HttpStatus.BAD_GATEWAY,
            "FILE_502_002",
            "파일 삭제에 실패했습니다."),

    PRESIGNED_DOWNLOAD_URL_GENERATION_FAILED(HttpStatus.BAD_GATEWAY,
            "FILE_502_003",
            "파일 조회 URL 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    FileErrorCode(HttpStatus httpStatus, String code, String message) {
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
