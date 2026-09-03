package com.quespot.domain.user.dto.res;

import com.quespot.domain.user.enums.Gender;
import com.quespot.domain.user.enums.TravelStyle;

import java.time.LocalDate;
import java.util.List;

public record ProfileResponseDTO(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        Gender gender,
        LocalDate birthDate,
        List<TravelStyle> travelStyles
) {
}
