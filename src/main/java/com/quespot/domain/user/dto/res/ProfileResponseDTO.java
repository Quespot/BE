package com.quespot.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.user.enums.Gender;
import com.quespot.domain.user.enums.ResidenceRegion;
import com.quespot.domain.user.enums.TravelCompanion;
import com.quespot.domain.user.enums.TravelStyle;

import java.time.LocalDate;
import java.util.List;

public record ProfileResponseDTO(
        Long userId,
        String email,
        String nickname,
        @Schema(description = "선택 항목. 없으면 null", nullable = true)
        String profileImageUrl,
        @Schema(description = "선택 항목. 없으면 null", nullable = true)
        Gender gender,
        LocalDate birthDate,
        ResidenceRegion residenceRegion,
        @Schema(description = "선택 항목. 없으면 null", nullable = true)
        TravelCompanion travelCompanion,
        List<TravelStyle> travelStyles
) {
}
