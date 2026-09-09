package com.quespot.domain.mission.service;

import com.quespot.domain.mission.enums.ArchivePhotoSource;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchiveCursorCodecTest {

    private final ArchiveCursorCodec codec = new ArchiveCursorCodec();

    @Test
    void encodeThenDecodeRoundTrips() {
        ArchiveCursor original = new ArchiveCursor(
                LocalDateTime.of(2026, 9, 10, 12, 30, 0), ArchivePhotoSource.MISSION, 42L
        );

        String encoded = codec.encode(original);
        ArchiveCursor decoded = codec.decode(encoded);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void encodeThenDecodeRoundTripsForArchiveSource() {
        ArchiveCursor original = new ArchiveCursor(
                LocalDateTime.of(2026, 9, 10, 12, 30, 0), ArchivePhotoSource.ARCHIVE, 7L
        );

        String encoded = codec.encode(original);
        ArchiveCursor decoded = codec.decode(encoded);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void decodeThrowsInvalidCursorForGarbageInput() {
        assertThatThrownBy(() -> codec.decode("not-base64!!"))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_CURSOR);
    }

    @Test
    void decodeThrowsInvalidCursorForWrongPartCount() {
        String malformed = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("2026-09-10T12:30".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decode(malformed))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_CURSOR);
    }

    @Test
    void decodeThrowsInvalidCursorForUnknownSource() {
        String malformed = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("2026-09-10T12:30|UNKNOWN|1".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decode(malformed))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_CURSOR);
    }
}
