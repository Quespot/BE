package com.quespot.domain.mission.dto.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ArrivalRequestDTO(
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude
) {
}
