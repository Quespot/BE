package com.quespot.domain.mission.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

public enum MissionErrorCode implements BaseErrorCode {

    CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MISSION_404_001",
            "미션 후보를 찾을 수 없습니다."),

    CANDIDATE_NOT_DRAFT(HttpStatus.CONFLICT,
            "MISSION_409_001",
            "대기 중인 미션 후보만 수정하거나 검수할 수 있습니다."),

    REVIEWER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MISSION_404_002",
            "검수자 정보를 찾을 수 없습니다."),

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MISSION_404_003",
            "미션을 찾을 수 없습니다."),

    INVALID_LOCATION(HttpStatus.BAD_REQUEST,
            "MISSION_400_001",
            "위도와 경도를 올바르게 입력해 주세요."),

    INVALID_CURSOR(HttpStatus.BAD_REQUEST,
            "MISSION_400_002",
            "유효하지 않은 미션 조회 커서입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    MissionErrorCode(HttpStatus httpStatus, String code, String message) {
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
