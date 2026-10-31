package com.quespot.domain.mission.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterArchivePhotoRequestDTO(
        @NotBlank String objectKey,
        @Size(max = 255) String caption
) {
}
