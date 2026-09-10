package com.quespot.domain.mission.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행정구역 스팟 완료 상태. COMPLETED = 해당 구의 미션을 전부 완료, INCOMPLETE = 남은 미션 있음")
public enum MissionSpotCompletionStatus {
    COMPLETED,
    INCOMPLETE
}
