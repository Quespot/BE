package com.quespot.domain.user.service;

import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class OAuth2TokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] encryptionKey;

    public OAuth2TokenCipher(
            @Value("${app.oauth2.token-encryption-key:}") String encodedKey,
            @Value("${app.oauth2.google.enabled:false}") boolean googleEnabled,
            @Value("${app.oauth2.kakao.enabled:false}") boolean kakaoEnabled,
            @Value("${app.oauth2.naver.enabled:false}") boolean naverEnabled
    ) {
        this.encryptionKey = decodeKey(encodedKey);
        if ((googleEnabled || kakaoEnabled || naverEnabled) && encryptionKey.length != 32) {
            throw new IllegalStateException(
                    "OAuth2 token encryption key must be a Base64-encoded 32-byte key."
            );
        }
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        validateConfiguredKey(AuthErrorCode.OAUTH2_LOGIN_FAILED);

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length)
                            .put(iv)
                            .put(encrypted)
                            .array()
            );
        } catch (GeneralSecurityException exception) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }
        validateConfiguredKey(AuthErrorCode.OAUTH2_UNLINK_FAILED);

        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new GeneralSecurityException("Invalid OAuth2 token payload.");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new AuthException(AuthErrorCode.OAUTH2_UNLINK_FAILED);
        }
    }

    private byte[] decodeKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            return new byte[0];
        }

        try {
            byte[] key = Base64.getDecoder().decode(encodedKey.trim());
            return key.length == 32 ? key : new byte[0];
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }

    private void validateConfiguredKey(AuthErrorCode errorCode) {
        if (encryptionKey.length != 32) {
            throw new AuthException(errorCode);
        }
    }
}
