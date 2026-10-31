package com.quespot.domain.user.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2TokenCipherTest {

    private static final String ENCODED_KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
    );

    @Test
    void encryptsAndDecryptsProviderToken() {
        OAuth2TokenCipher cipher = new OAuth2TokenCipher(
                ENCODED_KEY,
                true,
                false,
                false
        );

        String encrypted = cipher.encrypt("provider-access-token");

        assertThat(encrypted).isNotEqualTo("provider-access-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("provider-access-token");
    }
}
