package com.quespot.domain.mission.enums;

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
