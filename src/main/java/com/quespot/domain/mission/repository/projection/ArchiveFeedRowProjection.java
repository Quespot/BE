package com.quespot.domain.mission.repository.projection;

import java.time.LocalDateTime;

// mission_photos + archive_photos를 UNION한 아카이브 통합 피드 한 행(#45 확장).
// source가 'ARCHIVE'인 행은 missionId/missionTitle/missionCategory/completedAt이
// 전부 null이다 — 미션과 무관하게 자유 업로드된 사진이라 그렇다.
public interface ArchiveFeedRowProjection {
    Long getId();
    String getSource();
    String getImageKey();
    String getCaption();
    Long getMissionId();
    String getMissionTitle();
    String getMissionCategory();
    LocalDateTime getCompletedAt();
    LocalDateTime getCreatedAt();
}
