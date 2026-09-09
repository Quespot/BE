package com.quespot.domain.mission.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MissionPhotoTest {

    @Test
    void recordSetsAllFieldsAndCreatedAt() {
        MissionAttempt attempt = mock(MissionAttempt.class);
        LocalDateTime takenAt = LocalDateTime.of(2026, 1, 1, 12, 0);

        MissionPhoto photo = MissionPhoto.record(
                attempt, "missions/1/abc.jpg", "좋았다",
                new BigDecimal("37.5"), new BigDecimal("127.0"), takenAt
        );

        assertThat(photo.getAttempt()).isEqualTo(attempt);
        assertThat(photo.getImageKey()).isEqualTo("missions/1/abc.jpg");
        assertThat(photo.getCaption()).isEqualTo("좋았다");
        assertThat(photo.getLatitude()).isEqualByComparingTo("37.5");
        assertThat(photo.getLongitude()).isEqualByComparingTo("127.0");
        assertThat(photo.getTakenAt()).isEqualTo(takenAt);
        assertThat(photo.getCreatedAt()).isNotNull();
    }
}
