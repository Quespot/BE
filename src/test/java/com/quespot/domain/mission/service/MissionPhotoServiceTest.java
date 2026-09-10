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
import com.quespot.global.file.exception.FileException;
import com.quespot.global.file.exception.code.FileErrorCode;
import com.quespot.global.file.service.FileService;
import com.quespot.global.file.storage.FileStorage;
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

    private static final String VALID_OBJECT_KEY = "missions/1/abc.jpg";

    private MissionAttemptRepository missionAttemptRepository;
    private MissionPhotoRepository missionPhotoRepository;
    private FileStorage fileStorage;
    private FileService fileService;
    private MissionPhotoService missionPhotoService;

    @BeforeEach
    void setUp() {
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        missionPhotoRepository = mock(MissionPhotoRepository.class);
        fileStorage = mock(FileStorage.class);
        fileService = new FileService(fileStorage);
        missionPhotoService = new MissionPhotoService(
                missionAttemptRepository, missionPhotoRepository, fileService
        );
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
                1L, 100L, VALID_OBJECT_KEY, "좋았다",
                new BigDecimal("37.6"), new BigDecimal("127.0"), null
        );

        assertThat(photo.getImageKey()).isEqualTo(VALID_OBJECT_KEY);
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
                1L, 100L, VALID_OBJECT_KEY, null, null, null, null
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
                1L, 100L, "profiles/1/a.jpg", null, null, null, null
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
                1L, 100L, "profiles/1/a.jpg", null, new BigDecimal("37.6"), null, null
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
                1L, 100L, "profiles/1/a.jpg", null, null, new BigDecimal("127.0"), null
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
                1L, 100L, "profiles/1/a.jpg", null, null, null, null
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
                1L, 100L, VALID_OBJECT_KEY, null, null, null, null
        )).isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.PHOTO_ALREADY_EXISTS);
    }

    @Test
    void throwsWhenObjectKeyIsWrongPurpose() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(missionPhotoRepository.existsByAttemptId(100L)).thenReturn(false);

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "profiles/1/abc.jpg", null, null, null, null
        )).isInstanceOf(FileException.class)
                .extracting(e -> ((FileException) e).getErrorCode())
                .isEqualTo(FileErrorCode.OBJECT_KEY_WRONG_PURPOSE);
    }

    @Test
    void throwsWhenObjectKeyBelongsToDifferentUser() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(missionPhotoRepository.existsByAttemptId(100L)).thenReturn(false);

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "missions/2/abc.jpg", null, null, null, null
        )).isInstanceOf(FileException.class)
                .extracting(e -> ((FileException) e).getErrorCode())
                .isEqualTo(FileErrorCode.OBJECT_KEY_OWNER_MISMATCH);
    }

    @Test
    void resolveViewUrlReturnsNullWhenPhotoIsNull() {
        assertThat(missionPhotoService.resolveViewUrl(null)).isNull();
    }

    @Test
    void resolveViewUrlDelegatesToFileStorageWithStoredKey() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        MissionPhoto photo = MissionPhoto.record(
                attempt, VALID_OBJECT_KEY, null, new BigDecimal("37.5665"), new BigDecimal("126.9780"), null
        );
        when(fileStorage.createPresignedDownloadUrl(VALID_OBJECT_KEY)).thenReturn("https://presigned-url");

        String viewUrl = missionPhotoService.resolveViewUrl(photo);

        assertThat(viewUrl).isEqualTo("https://presigned-url");
    }
}
