package com.quespot.domain.mission.service;

import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class MissionCursorCodec {

    private static final String DELIMITER = "|";

    public String encode(MissionCursor cursor) {
        String value = String.join(
                DELIMITER,
                cursor.sortMode().name(),
                Long.toString(cursor.seed()),
                Double.toString(cursor.sortValue()),
                Long.toString(cursor.missionId()),
                cursor.querySignature()
        );
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public MissionCursor decode(String value) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(value),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 5) {
                throw invalidCursor();
            }

            MissionCursor cursor = new MissionCursor(
                    MissionCursor.SortMode.valueOf(parts[0]),
                    Long.parseLong(parts[1]),
                    Double.parseDouble(parts[2]),
                    Long.parseLong(parts[3]),
                    parts[4]
            );
            if (!Double.isFinite(cursor.sortValue())
                    || cursor.sortValue() < 0
                    || cursor.missionId() <= 0) {
                throw invalidCursor();
            }
            return cursor;
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    public String querySignature(
            Long userId,
            MissionCategory category,
            String keyword,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        String value = String.join(
                DELIMITER,
                userId.toString(),
                category == null ? "" : category.name(),
                keyword == null ? "" : keyword,
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

    private String normalize(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private MissionException invalidCursor() {
        return new MissionException(MissionErrorCode.INVALID_CURSOR);
    }
}
