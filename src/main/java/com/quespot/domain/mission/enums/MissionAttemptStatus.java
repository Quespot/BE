package com.quespot.domain.mission.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미션 시도 상태. IN_PROGRESS = 시작 후 인증 전, COMPLETED = GPS 인증 완료(포인트 지급됨), QUIT = 포기")
public enum MissionAttemptStatus {
    IN_PROGRESS,
    COMPLETED,
    QUIT
}
