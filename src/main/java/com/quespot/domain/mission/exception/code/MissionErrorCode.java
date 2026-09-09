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
            "유효하지 않은 미션 조회 커서입니다."),

    ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MISSION_404_004",
            "미션 시도를 찾을 수 없습니다."),

    MISSION_ALREADY_COMPLETED(HttpStatus.CONFLICT,
            "MISSION_409_002",
            "이미 완료한 미션은 다시 시작할 수 없습니다."),

    ATTEMPT_NOT_IN_PROGRESS(HttpStatus.CONFLICT,
            "MISSION_409_003",
            "진행 중인 시도가 아닙니다."),

    ATTEMPT_QUIT(HttpStatus.CONFLICT,
            "MISSION_409_004",
            "이미 종료된 시도입니다. 새로 시작해 주세요."),

    PHOTO_ATTEMPT_NOT_COMPLETED(HttpStatus.CONFLICT,
            "MISSION_409_006",
            "완료된 미션에만 사진을 기록할 수 있습니다."),

    PHOTO_ALREADY_EXISTS(HttpStatus.CONFLICT,
            "MISSION_409_005",
            "이미 사진을 기록했습니다."),

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MISSION_404_005",
            "미션 코스를 찾을 수 없습니다."),

    COURSE_ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MISSION_404_006",
            "코스 진행을 찾을 수 없습니다."),

    COURSE_ALREADY_COMPLETED(HttpStatus.CONFLICT,
            "MISSION_409_007",
            "이미 완주한 코스는 다시 시작할 수 없습니다."),

    COURSE_ATTEMPT_NOT_IN_PROGRESS(HttpStatus.CONFLICT,
            "MISSION_409_008",
            "진행 중인 코스가 아닙니다."),

    MISSION_NOT_IN_COURSE(HttpStatus.BAD_REQUEST,
            "MISSION_400_003",
            "해당 코스에 속하지 않는 미션입니다."),

    ANCHOR_NOT_AVAILABLE(HttpStatus.CONFLICT,
            "MISSION_409_009",
            "이미 시작했거나 완료한 미션은 코스 앵커로 선택할 수 없습니다."),

    COURSE_GENERATION_FAILED(HttpStatus.CONFLICT,
            "MISSION_409_010",
            "주변에 이어갈 미션이 부족해 코스를 만들 수 없습니다."),

    MISSION_LOCKED(HttpStatus.CONFLICT,
            "MISSION_409_011",
            "아직 잠금 해제되지 않은 미션입니다. 이전 미션을 먼저 완료해 주세요.");

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
