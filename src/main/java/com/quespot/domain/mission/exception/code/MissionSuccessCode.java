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

    MISSIONS_FOUND(HttpStatus.OK,
            "MISSION_200_007",
            "미션 목록을 조회했습니다."),

    MISSION_FOUND(HttpStatus.OK,
            "MISSION_200_008",
            "미션을 조회했습니다."),

    MISSIONS_PUBLISHED(HttpStatus.CREATED,
            "MISSION_201_001",
            "미션을 일괄 발행했습니다."),

    MISSION_ATTEMPT_STARTED(HttpStatus.OK,
            "MISSION_200_009",
            "미션을 시작했습니다."),

    MISSION_ATTEMPTS_FOUND(HttpStatus.OK,
            "MISSION_200_010",
            "진행 중인 미션 시도 목록을 조회했습니다."),

    MISSION_ATTEMPT_FOUND(HttpStatus.OK,
            "MISSION_200_011",
            "미션 시도를 조회했습니다."),

    MISSION_ATTEMPT_ARRIVED(HttpStatus.OK,
            "MISSION_200_012",
            "도착 인증 결과를 확인했습니다."),

    MISSION_ATTEMPT_GUIDE_FOUND(HttpStatus.OK,
            "MISSION_200_013",
            "인증 가이드를 조회했습니다."),

    MISSION_ATTEMPT_RESULT_FOUND(HttpStatus.OK,
            "MISSION_200_014",
            "완료 결과를 조회했습니다."),

    MISSION_PHOTO_REGISTERED(HttpStatus.OK,
            "MISSION_200_015",
            "사진을 기록했습니다."),

    MISSION_REFLECTION_REGISTERED(HttpStatus.OK,
            "MISSION_200_016",
            "감상을 기록했습니다."),

    MISSION_ATTEMPT_QUIT(HttpStatus.OK,
            "MISSION_200_017",
            "미션 시도를 종료했습니다."),

    COURSES_FOUND(HttpStatus.OK,
            "MISSION_200_018",
            "미션 코스 목록을 조회했습니다."),

    COURSE_FOUND(HttpStatus.OK,
            "MISSION_200_019",
            "미션 코스를 조회했습니다."),

    COURSE_ATTEMPT_STARTED(HttpStatus.OK,
            "MISSION_200_020",
            "코스를 시작했습니다."),

    COURSE_ATTEMPTS_FOUND(HttpStatus.OK,
            "MISSION_200_021",
            "진행 중인 코스 목록을 조회했습니다."),

    COURSE_ATTEMPT_QUIT(HttpStatus.OK,
            "MISSION_200_022",
            "코스를 포기했습니다."),

    COURSE_GENERATED(HttpStatus.CREATED,
            "MISSION_201_002",
            "미션 코스를 생성했습니다."),

    UNLOCK_CONDITION_FOUND(HttpStatus.OK,
            "MISSION_200_023",
            "잠금 조건을 조회했습니다."),

    ARCHIVES_FOUND(HttpStatus.OK,
            "MISSION_200_024",
            "아카이브 목록을 조회했습니다."),

    ARCHIVE_PHOTO_REGISTERED(HttpStatus.CREATED,
            "MISSION_201_003",
            "아카이브에 사진을 등록했습니다.");

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
