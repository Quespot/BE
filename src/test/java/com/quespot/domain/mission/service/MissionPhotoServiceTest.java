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
import com.quespot.global.s3.config.S3Properties;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import com.quespot.global.s3.service.S3ImageUrlValidator;
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

    private static final String VALID_IMAGE_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/1/abc.jpg";

    private MissionAttemptRepository missionAttemptRepository;
    private MissionPhotoRepository missionPhotoRepository;
    private MissionPhotoService missionPhotoService;

    @BeforeEach
    void setUp() {
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        missionPhotoRepository = mock(MissionPhotoRepository.class);
        // 실제 인스턴스 — URL 패턴 검증은 순수 로직이라 목킹 없이 그대로 검증 가능.
        S3ImageUrlValidator s3ImageUrlValidator =
                new S3ImageUrlValidator(new S3Properties("test-bucket", "ap-northeast-2", 300, 10_485_760));
        missionPhotoService = new MissionPhotoService(missionAttemptRepository, missionPhotoRepository, s3ImageUrlValidator);
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
                1L, 100L, VALID_IMAGE_URL, "좋았다",
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
                1L, 100L, VALID_IMAGE_URL, null, null, null, null
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
                1L, 100L, VALID_IMAGE_URL, null, null, null, null
        )).isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.PHOTO_ALREADY_EXISTS);
    }

    @Test
    void throwsWhenImageUrlIsNotOurBucket() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(missionPhotoRepository.existsByAttemptId(100L)).thenReturn(false);

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "https://example.com/a.jpg", null, null, null, null
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.IMAGE_URL_NOT_OUR_BUCKET);
    }

    @Test
    void throwsWhenImageUrlBelongsToDifferentUser() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));
        when(missionPhotoRepository.existsByAttemptId(100L)).thenReturn(false);

        assertThatThrownBy(() -> missionPhotoService.registerPhoto(
                1L, 100L, "https://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/2/abc.jpg",
                null, null, null, null
        )).isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.IMAGE_URL_OWNER_MISMATCH);
    }
}
