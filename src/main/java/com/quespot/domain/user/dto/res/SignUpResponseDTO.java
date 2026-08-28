package com.quespot.domain.user.dto.res;

import com.quespot.domain.user.entity.User;

public record SignUpResponseDTO(
        Long userId,
        String email,
        String nickname
) {

    public static SignUpResponseDTO from(User user) {
        return new SignUpResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
