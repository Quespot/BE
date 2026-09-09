package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.ArrivalResultDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.reward.service.PointService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionArrivalServiceTest {

    private MissionAttemptRepository missionAttemptRepository;
    private PointService pointService;
    private CourseAttemptService courseAttemptService;
    private MissionArrivalService missionArrivalService;

    @BeforeEach
    void setUp() {
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        pointService = mock(PointService.class);
        courseAttemptService = mock(CourseAttemptService.class);
        missionArrivalService = new MissionArrivalService(
                missionAttemptRepository, pointService, new GeoDistanceCalculator(), courseAttemptService
        );
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

    private MissionAttempt attemptOwnedBy(Long userId, Mission mission) {
        return MissionAttempt.start(userId, mission);
    }

    @Test
    void arrivalWithinRadiusCompletesAndCreditsPoints() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = attemptOwnedBy(1L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        ArrivalResultDTO result = missionArrivalService.arrive(1L, 100L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo(MissionAttemptStatus.COMPLETED);
        assertThat(result.earnedPoint()).isEqualTo(mission.getRewardPoint());
        verify(pointService).credit(anyLong(), anyInt(), anyString(), anyString(), any(), anyString());
        verify(courseAttemptService, never()).tryCompleteViaMissionCompletion(any());
    }

    @Test
    void arrivalWithCourseAttemptIdTriggersCourseCompletionCheck() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = MissionAttempt.start(1L, mission, 55L);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        missionArrivalService.arrive(1L, 100L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        verify(courseAttemptService).tryCompleteViaMissionCompletion(55L);
    }

    @Test
    void arrivalOutsideRadiusDoesNotChangeStatusOrCreditPoints() {
        // 위도 1도 ≈ 111km 차이 → 확실히 500m 밖.
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = attemptOwnedBy(1L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        ArrivalResultDTO result = missionArrivalService.arrive(1L, 100L, new BigDecimal("38.5665"), new BigDecimal("126.9780"));

        assertThat(result.success()).isFalse();
        assertThat(result.distanceMeters()).isGreaterThan(500);
        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.IN_PROGRESS);
        verify(pointService, never()).credit(anyLong(), anyInt(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void arrivalOnAlreadyCompletedAttemptReturnsExistingResultIdempotently() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = attemptOwnedBy(1L, mission);
        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), mission.getRewardPoint());
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        ArrivalResultDTO result = missionArrivalService.arrive(1L, 100L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo(MissionAttemptStatus.COMPLETED);
        verify(pointService, never()).credit(anyLong(), anyInt(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void arrivalOnQuitAttemptThrows() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = attemptOwnedBy(1L, mission);
        attempt.quit();
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionArrivalService.arrive(1L, 100L, new BigDecimal("37.5665"), new BigDecimal("126.9780")))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.ATTEMPT_QUIT);
    }

    @Test
    void arrivalAtExactlyRadiusMetersSucceeds() {
        // 정확히 500m 지점에서의 경계값(> vs >=)을 실제 좌표 오차 없이 검증하기
        // 위해 GeoDistanceCalculator를 스텁으로 대체한다.
        GeoDistanceCalculator stubCalculator = mock(GeoDistanceCalculator.class);
        when(stubCalculator.distanceMeters(any(), any(), any(), any()))
                .thenReturn((long) MissionArrivalService.RADIUS_METERS);
        MissionArrivalService service = new MissionArrivalService(missionAttemptRepository, pointService, stubCalculator, courseAttemptService);
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = attemptOwnedBy(1L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        ArrivalResultDTO result = service.arrive(1L, 100L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        assertThat(result.success()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.COMPLETED);
    }

    @Test
    void arrivalOneMeterBeyondRadiusFails() {
        GeoDistanceCalculator stubCalculator = mock(GeoDistanceCalculator.class);
        when(stubCalculator.distanceMeters(any(), any(), any(), any()))
                .thenReturn((long) MissionArrivalService.RADIUS_METERS + 1);
        MissionArrivalService service = new MissionArrivalService(missionAttemptRepository, pointService, stubCalculator, courseAttemptService);
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = attemptOwnedBy(1L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        ArrivalResultDTO result = service.arrive(1L, 100L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        assertThat(result.success()).isFalse();
        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.IN_PROGRESS);
    }

    @Test
    void arrivalOnAttemptNotOwnedByUserThrows() {
        Mission mission = missionAt("37.5665", "126.9780");
        MissionAttempt attempt = attemptOwnedBy(2L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionArrivalService.arrive(1L, 100L, new BigDecimal("37.5665"), new BigDecimal("126.9780")))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.ATTEMPT_NOT_FOUND);
    }
}
