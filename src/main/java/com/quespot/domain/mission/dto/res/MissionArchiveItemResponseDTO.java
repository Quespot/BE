package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.ArchivePhotoSource;
import com.quespot.domain.mission.enums.MissionCategory;

import java.time.LocalDateTime;

public record MissionArchiveItemResponseDTO(
        Long photoId,
        ArchivePhotoSource source,
        String imageUrl,
        String caption,
        Long missionId,
        String missionTitle,
        MissionCategory missionCategory,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
