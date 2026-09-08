package com.quespot.domain.mission.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectMissionCandidateRequestDTO(
        @NotBlank(message = "반려 사유는 필수입니다.")
        @Size(max = 1000, message = "반려 사유는 1000자 이하여야 합니다.")
        String reason
) {
}
