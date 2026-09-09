package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionPhotoRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MissionPhotoServiceTest {

    private MissionAttemptRepository missionAttemptRepository;
    private MissionPhotoRepository missionPhotoRepository;
    private MissionPhotoService missionPhotoService;

    @BeforeEach
    void setUp() {
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        missionPhotoRepository = mock(MissionPhotoRepository.class);
        missionPhotoService = new MissionPhotoService(missionAttemptRepository, missionPhotoRepository);
        when(missionPhotoRepository.save(any(MissionPhoto.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Mission missionAt(String lat, String lng) {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("1").name("테스트 스팟")
                .latitude(new BigDecimal(lat)).longitude(new BigDecimal(lng))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        MissionCandidate candidate = MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1);
        return Mission.publish(candidate);
    }

    @Test
    void registersPhotoWithSubmittedCoordinatesWhenCompleted() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(missionPhotoRepository.existsByAttemptId(100L)).thenReturn(false);

        MissionPhoto photo = missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", "좋았다",
                new BigDecimal("37.6"), new BigDecimal("127.0"), null
        );

        assertThat(photo.getLatitude()).isEqualByComparingTo("37.6");
        assertThat(photo.getLongitude()).isEqualByComparingTo("127.0");
    }

    @Test
    void fallsBackToMissionSnapshotCoordinatesWhenNotProvided() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(missionPhotoRepository.existsByAttemptId(100L)).thenReturn(false);

        MissionPhoto photo = missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", null, null, null, null
        );

        assertThat(photo.getLatitude()).isEqualByComparingTo("37.5665");
        assertThat(photo.getLongitude()).isEqualByComparingTo("126.9780");
    }

    @Test
    void throwsWhenAttemptNotCompleted() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", null, null, null, null
        )).isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.PHOTO_ATTEMPT_NOT_COMPLETED);
    }

    @Test
    void throwsWhenOnlyLatitudeIsProvided() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", null, new BigDecimal("37.6"), null, null
        )).isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_LOCATION);
    }

    @Test
    void throwsWhenOnlyLongitudeIsProvided() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", null, null, new BigDecimal("127.0"), null
        )).isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_LOCATION);
    }

    @Test
    void throwsWhenAttemptNotOwnedByUser() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(2L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", null, null, null, null
        )).isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.ATTEMPT_NOT_FOUND);
    }

    @Test
    void throwsWhenPhotoAlreadyExists() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(missionPhotoRepository.existsByAttemptId(100L)).thenReturn(true);

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", null, null, null, null
        )).isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.PHOTO_ALREADY_EXISTS);
    }
}
