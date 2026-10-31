package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.ArchivePhotoSource;
import com.quespot.domain.mission.enums.MissionCategory;

import java.time.LocalDateTime;

public record MissionArchiveItemResponseDTO(
        @Schema(description = "source별로 다른 테이블의 id라 값이 겹칠 수 있다. 항목 식별은 source + photoId 조합으로")
        Long photoId,
        @Schema(description = "MISSION = 미션 완료 후 등록한 사진, ARCHIVE = 미션과 무관한 자유 업로드")
        ArchivePhotoSource source,
        @Schema(description = "매번 새로 서명한 presigned URL. 저장하지 말 것")
        String imageUrl,
        @Schema(description = "사용자가 안 적었으면 null", nullable = true)
        String caption,
        @Schema(description = "source=ARCHIVE면 null", nullable = true)
        Long missionId,
        @Schema(description = "source=ARCHIVE면 null", nullable = true)
        String missionTitle,
        @Schema(description = "source=ARCHIVE면 null", nullable = true)
        MissionCategory missionCategory,
        @Schema(description = "미션 완료 시각. source=ARCHIVE면 null", nullable = true)
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
