package com.quespot.global.security.principal;

import com.quespot.domain.user.enums.UserRole;

public record AuthenticatedUser(
        Long userId,
        UserRole role,
        String sessionId
) {
}
