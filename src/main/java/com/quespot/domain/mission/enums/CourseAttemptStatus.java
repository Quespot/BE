package com.quespot.domain.mission.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 진행 상태. IN_PROGRESS = 진행 중, COMPLETED = 구성 미션 전부 완료(보너스 지급됨), QUIT = 포기")
public enum CourseAttemptStatus {
    IN_PROGRESS,
    COMPLETED,
    QUIT
}
