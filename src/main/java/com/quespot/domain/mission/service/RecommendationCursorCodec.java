package com.quespot.domain.mission.service;

import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class RecommendationCursorCodec {

    private static final String DELIMITER = "|";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HMAC_CONTEXT = "mission-recommendation-cursor:v1|";

    private final SecretKeySpec secretKey;

    public RecommendationCursorCodec(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public String encode(RecommendationCursor cursor) {
        String payload = String.join(
                DELIMITER,
                cursor.sortMode().name(),
                Long.toString(cursor.seed()),
                Integer.toString(cursor.preferenceRank()),
                Long.toString(cursor.categoryRank()),
                Long.toString(cursor.categoryOrder()),
                Double.toString(cursor.sortValue()),
                Long.toString(cursor.missionId()),
                cursor.querySignature()
        );
        String value = payload + DELIMITER + encodeMac(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public RecommendationCursor decode(String value) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 9) {
                throw invalidCursor();
            }

            String payload = String.join(
                    DELIMITER,
                    parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]
            );
            byte[] providedMac = Base64.getUrlDecoder().decode(parts[8]);
            if (!MessageDigest.isEqual(createMac(payload), providedMac)) {
                throw invalidCursor();
            }

            RecommendationCursor cursor = new RecommendationCursor(
                    RecommendationCursor.SortMode.valueOf(parts[0]),
                    Long.parseLong(parts[1]),
                    Integer.parseInt(parts[2]),
                    Long.parseLong(parts[3]),
                    Long.parseLong(parts[4]),
                    Double.parseDouble(parts[5]),
                    Long.parseLong(parts[6]),
                    parts[7]
            );
            validate(cursor);
            return cursor;
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    public String querySignature(
            Long userId,
            String preferredCategories,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        String value = String.join(
                DELIMITER,
                userId.toString(),
                preferredCategories,
                normalize(latitude),
                normalize(longitude)
        );
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private void validate(RecommendationCursor cursor) {
        if (!Double.isFinite(cursor.sortValue())
                || cursor.sortValue() < 0
                || cursor.preferenceRank() < 0
                || cursor.categoryRank() <= 0
                || cursor.categoryOrder() < 0
                || cursor.missionId() <= 0) {
            throw invalidCursor();
        }
    }

    private String encodeMac(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(createMac(payload));
    }

    private byte[] createMac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            return mac.doFinal((HMAC_CONTEXT + payload).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 algorithm is unavailable", exception);
        }
    }

    private String normalize(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private MissionException invalidCursor() {
        return new MissionException(MissionErrorCode.INVALID_CURSOR);
    }
}
