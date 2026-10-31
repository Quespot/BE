package com.quespot.domain.mission.dto.req;

import com.quespot.domain.mission.enums.MissionCategory;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMissionCandidateRequestDTO(
        @Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\P{javaWhitespace}.*", message = "제목은 공백으로만 구성할 수 없습니다.")
        String title,

        @Size(min = 1, max = 2000, message = "설명은 1자 이상 2000자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\P{javaWhitespace}.*", message = "설명은 공백으로만 구성할 수 없습니다.")
        String description,

        MissionCategory category,

        @Positive(message = "보상 포인트는 양수여야 합니다.")
        Integer rewardPoint,

        @Positive(message = "예상 시간은 양수여야 합니다.")
        Integer estimatedMinutes
) {
}
