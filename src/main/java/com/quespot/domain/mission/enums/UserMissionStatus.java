package com.quespot.domain.mission.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 기준 미션 상태. AVAILABLE = 시작 가능, IN_PROGRESS = 진행 중, COMPLETED = 완료, LOCKED = 진행 중 코스에서 앞 순서 미션이 미완료라 시작 불가")
public enum UserMissionStatus {
    AVAILABLE(true, false),
    IN_PROGRESS(false, false),
    COMPLETED(false, true),
    LOCKED(false, false);

    private final boolean canStart;
    private final boolean canCreateArchive;

    UserMissionStatus(boolean canStart, boolean canCreateArchive) {
        this.canStart = canStart;
        this.canCreateArchive = canCreateArchive;
    }

    public boolean canStart() {
        return canStart;
    }

    public boolean canCreateArchive() {
        return canCreateArchive;
    }
}
