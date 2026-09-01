package com.quespot.domain.user.dto.res;

import com.quespot.domain.user.enums.TravelStyle;

import java.util.List;

public record ProfileResponseDTO(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        List<TravelStyle> travelStyles
) {
}
