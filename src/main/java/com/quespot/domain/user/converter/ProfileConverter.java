package com.quespot.domain.user.converter;

import com.quespot.domain.user.dto.res.ProfileResponseDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.TravelStyle;

import java.util.Comparator;

public final class ProfileConverter {

    private ProfileConverter() {
    }

    public static ProfileResponseDTO toProfileResponseDTO(User user) {
        return new ProfileResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getTravelStyles().stream()
                        .sorted(Comparator.comparingInt(TravelStyle::ordinal))
                        .toList()
        );
    }
}
