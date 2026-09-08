package com.quespot.domain.mission.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum MissionSuccessCode implements BaseCode {

    CANDIDATES_GENERATED(HttpStatus.OK,
            "MISSION_200_001",
            "미션 후보 생성을 완료했습니다."),

    CANDIDATES_FOUND(HttpStatus.OK,
            "MISSION_200_002",
            "미션 후보 목록을 조회했습니다."),

    CANDIDATE_FOUND(HttpStatus.OK,
            "MISSION_200_003",
            "미션 후보를 조회했습니다."),

    CANDIDATE_UPDATED(HttpStatus.OK,
            "MISSION_200_004",
            "미션 후보를 수정했습니다."),

    CANDIDATES_APPROVED(HttpStatus.OK,
            "MISSION_200_005",
            "미션 후보를 일괄 승인했습니다."),

    CANDIDATE_REJECTED(HttpStatus.OK,
            "MISSION_200_006",
            "미션 후보를 반려했습니다."),

    MISSIONS_PUBLISHED(HttpStatus.CREATED,
            "MISSION_201_001",
            "미션을 일괄 발행했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    MissionSuccessCode(HttpStatus httpStatus, String code, String message) {
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
