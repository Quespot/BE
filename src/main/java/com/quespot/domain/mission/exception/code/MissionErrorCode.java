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
            "검수자 정보를 찾을 수 없습니다.");

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
