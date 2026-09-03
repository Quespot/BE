package com.quespot.domain.user.converter;

import com.quespot.domain.user.dto.res.ProfileResponseDTO;
import com.quespot.domain.user.entity.UserProfile;
import com.quespot.domain.user.enums.TravelStyle;

import java.util.Comparator;

public final class ProfileConverter {

    private ProfileConverter() {
    }

    public static ProfileResponseDTO toProfileResponseDTO(UserProfile profile) {
        return new ProfileResponseDTO(
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getNickname(),
                profile.getProfileImageUrl(),
                profile.getGender(),
                profile.getBirthDate(),
                profile.getTravelStyles().stream()
                        .sorted(Comparator.comparingInt(TravelStyle::ordinal))
                        .toList()
        );
    }
}
