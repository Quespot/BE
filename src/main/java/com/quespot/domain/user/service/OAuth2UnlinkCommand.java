package com.quespot.domain.user.service;

import com.quespot.domain.user.enums.LoginProvider;

public record OAuth2UnlinkCommand(
        LoginProvider provider,
        String providerUserId,
        String encryptedAccessToken,
        String encryptedRefreshToken
) {
}
