package com.quespot.domain.mission.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReflectionRequestDTO(
        @NotBlank @Size(max = 1000) String content
) {
}
