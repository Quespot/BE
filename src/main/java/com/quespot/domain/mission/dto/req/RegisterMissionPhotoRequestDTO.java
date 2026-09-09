package com.quespot.domain.mission.dto.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegisterMissionPhotoRequestDTO(
        @NotBlank String imageUrl,
        @Size(max = 255) String caption,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        LocalDateTime takenAt
) {
}
