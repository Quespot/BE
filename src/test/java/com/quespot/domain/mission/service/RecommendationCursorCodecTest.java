package com.quespot.domain.mission.service;

import com.quespot.domain.mission.exception.MissionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationCursorCodecTest {

    private static final String SECRET = "recommendation-cursor-test-secret";

    private final RecommendationCursorCodec codec = new RecommendationCursorCodec(SECRET);

    @Test
    void roundTripsSignedCursor() {
        RecommendationCursor cursor = new RecommendationCursor(
                RecommendationCursor.SortMode.DISTANCE,
                123L,
                0,
                2L,
                100L,
                1250.5,
                7L,
                "query-signature"
        );

        RecommendationCursor decoded = codec.decode(codec.encode(cursor));

        assertThat(decoded).isEqualTo(cursor);
    }

    @Test
    void rejectsTamperedCursorPayload() {
        RecommendationCursor cursor = new RecommendationCursor(
                RecommendationCursor.SortMode.RANDOM,
                123L,
                0,
                1L,
                100L,
                200.0,
                7L,
                "query-signature"
        );
        String encoded = codec.encode(cursor);
        String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        String tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(
                decoded.replace("|200.0|", "|201.0|").getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> codec.decode(tampered)).isInstanceOf(MissionException.class);
    }

    @Test
    void querySignatureNormalizesEquivalentCoordinates() {
        String first = codec.querySignature(
                1L, "FOOD,NATURE", new BigDecimal("37.500"), new BigDecimal("127.000")
        );
        String second = codec.querySignature(
                1L, "FOOD,NATURE", new BigDecimal("37.5"), new BigDecimal("127")
        );

        assertThat(first).isEqualTo(second);
    }
}
