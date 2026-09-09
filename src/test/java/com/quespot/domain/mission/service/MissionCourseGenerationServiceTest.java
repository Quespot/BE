package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.MissionCourseDetailResultDTO;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCourseCandidateRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.mission.repository.projection.CourseCandidateMissionProjection;
import com.quespot.domain.mission.repository.projection.ExistingCoursePairProjection;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionCourseGenerationServiceTest {

    private MissionCourseCandidateRepository candidateRepository;
    private MissionCourseRepository missionCourseRepository;
    private CourseMissionRepository courseMissionRepository;
    private MissionAttemptRepository missionAttemptRepository;
    private CourseAttemptService courseAttemptService;
    private CourseLockPolicy courseLockPolicy;
    private MissionCourseGenerationService service;

    @BeforeEach
    void setUp() {
        candidateRepository = mock(MissionCourseCandidateRepository.class);
        missionCourseRepository = mock(MissionCourseRepository.class);
        courseMissionRepository = mock(CourseMissionRepository.class);
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        courseAttemptService = mock(CourseAttemptService.class);
        // 실제 인스턴스 — resolveCourseMissionStatuses(courseMissions, Set.of(), Set.of())는
        // 순수 계산이라 목킹 없이 그대로 검증 가능(방금 생성한 코스라 완료/진행중 없음).
        courseLockPolicy = new CourseLockPolicy(courseMissionRepository, missionAttemptRepository);
        service = new MissionCourseGenerationService(
                candidateRepository, missionCourseRepository, courseMissionRepository,
                missionAttemptRepository, courseAttemptService, courseLockPolicy
        );
    }

    private Mission mission(Long id, String name, MissionTemplate template, String lat, String lng) {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("g" + id)
                .name(name)
                .latitude(new BigDecimal(lat)).longitude(new BigDecimal(lng))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        Mission m = Mission.publish(MissionCandidate.generate(spot, template, 1));
        setId(m, id);
        return m;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setCourseId(MissionCourse course, Long id) {
        try {
            var field = MissionCourse.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(course, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CourseCandidateMissionProjection candidate(Long id, String lat, String lng) {
        CourseCandidateMissionProjection p = mock(CourseCandidateMissionProjection.class);
        when(p.getMissionId()).thenReturn(id);
        when(p.getLatitude()).thenReturn(new BigDecimal(lat));
        when(p.getLongitude()).thenReturn(new BigDecimal(lng));
        return p;
    }

    @Test
    void generateAndStartThrowsAnchorNotAvailableWhenAlreadyInProgress() {
        Mission anchor = mission(1L, "경복궁", MissionTemplate.CULTURE_LOCATION, "37.5665", "126.9780");
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(anchor));
        var attemptedProjection = mock(com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection.class);
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(9L, List.of(1L), List.of(
                MissionAttemptStatus.IN_PROGRESS, MissionAttemptStatus.COMPLETED
        ))).thenReturn(List.of(attemptedProjection));

        assertThatThrownBy(() -> service.generateAndStart(9L, 1L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.ANCHOR_NOT_AVAILABLE);
    }

    @Test
    void generateAndStartThrowsMissionNotFoundWhenAnchorMissing() {
        when(candidateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateAndStart(9L, 1L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_NOT_FOUND);
    }

    @Test
    void generateAndStartBuildsCourseFromNearestValidPairAt500m() {
        Mission anchor = mission(1L, "경복궁", MissionTemplate.CULTURE_LOCATION, "37.5665", "126.9780");
        Mission m2 = mission(2L, "북촌한옥마을", MissionTemplate.NATURE_LOCATION, "37.5701", "126.9780");
        Mission m3 = mission(3L, "인사동", MissionTemplate.CULTURE_LOCATION, "37.5715", "126.9850");
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(anchor));
        when(candidateRepository.findById(2L)).thenReturn(Optional.of(m2));
        when(candidateRepository.findById(3L)).thenReturn(Optional.of(m3));
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(9L, List.of(1L), List.of(
                MissionAttemptStatus.IN_PROGRESS, MissionAttemptStatus.COMPLETED
        ))).thenReturn(List.of());
        when(courseMissionRepository.findExistingPairs(9L, 1L)).thenReturn(List.of());
        CourseCandidateMissionProjection candidateM2 = candidate(2L, "37.5701", "126.9780");
        CourseCandidateMissionProjection candidateM3 = candidate(3L, "37.5715", "126.9850");
        when(candidateRepository.findNearestCandidates(
                eq(anchor.getSnapshotLatitude()), eq(anchor.getSnapshotLongitude()), eq(500),
                any(), any(), eq(9L), anyInt()
        )).thenReturn(List.of(candidateM2));
        when(candidateRepository.findNearestCandidates(
                eq(m2.getSnapshotLatitude()), eq(m2.getSnapshotLongitude()), eq(500),
                any(), any(), eq(9L), anyInt()
        )).thenReturn(List.of(candidateM3));
        when(missionCourseRepository.save(any(MissionCourse.class))).thenAnswer(inv -> {
            MissionCourse c = inv.getArgument(0);
            setCourseId(c, 100L);
            return c;
        });
        when(courseMissionRepository.save(any(CourseMission.class))).thenAnswer(inv -> inv.getArgument(0));

        MissionCourseDetailResultDTO result = service.generateAndStart(9L, 1L);

        assertThat(result.course().getName()).isEqualTo("경복궁 주변 코스");
        assertThat(result.course().getTotalRewardPoint()).isEqualTo(
                anchor.getRewardPoint() + m2.getRewardPoint() + m3.getRewardPoint()
        );
        assertThat(result.course().getBonusPoint()).isEqualTo(
                Math.round(result.course().getTotalRewardPoint() * 0.30f)
        );
        assertThat(result.missions()).hasSize(3);
        verify(courseAttemptService).start(9L, 100L);
    }

    @Test
    void generateAndStartWidensTo1kmWhenNoCandidateWithin500m() {
        Mission anchor = mission(1L, "경복궁", MissionTemplate.CULTURE_LOCATION, "37.5665", "126.9780");
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(anchor));
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(9L, List.of(1L), List.of(
                MissionAttemptStatus.IN_PROGRESS, MissionAttemptStatus.COMPLETED
        ))).thenReturn(List.of());
        when(courseMissionRepository.findExistingPairs(9L, 1L)).thenReturn(List.of());
        when(candidateRepository.findNearestCandidates(
                eq(anchor.getSnapshotLatitude()), eq(anchor.getSnapshotLongitude()), eq(500),
                any(), any(), eq(9L), anyInt()
        )).thenReturn(List.of());
        when(candidateRepository.findNearestCandidates(
                eq(anchor.getSnapshotLatitude()), eq(anchor.getSnapshotLongitude()), eq(1000),
                any(), any(), eq(9L), anyInt()
        )).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateAndStart(9L, 1L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.COURSE_GENERATION_FAILED);

        verify(candidateRepository).findNearestCandidates(
                eq(anchor.getSnapshotLatitude()), eq(anchor.getSnapshotLongitude()), eq(500),
                any(), any(), eq(9L), anyInt()
        );
        verify(candidateRepository).findNearestCandidates(
                eq(anchor.getSnapshotLatitude()), eq(anchor.getSnapshotLongitude()), eq(1000),
                any(), any(), eq(9L), anyInt()
        );
    }

    @Test
    void generateAndStartSkipsPairThatMatchesExistingCourseAndTriesNextCandidate() {
        Mission anchor = mission(1L, "경복궁", MissionTemplate.CULTURE_LOCATION, "37.5665", "126.9780");
        Mission m2First = mission(2L, "가까운후보", MissionTemplate.NATURE_LOCATION, "37.5670", "126.9780");
        Mission m2Second = mission(4L, "다음후보", MissionTemplate.NATURE_LOCATION, "37.5680", "126.9780");
        Mission m3 = mission(3L, "인사동", MissionTemplate.CULTURE_LOCATION, "37.5715", "126.9850");
        when(candidateRepository.findById(1L)).thenReturn(Optional.of(anchor));
        when(candidateRepository.findById(4L)).thenReturn(Optional.of(m2Second));
        when(candidateRepository.findById(3L)).thenReturn(Optional.of(m3));
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(9L, List.of(1L), List.of(
                MissionAttemptStatus.IN_PROGRESS, MissionAttemptStatus.COMPLETED
        ))).thenReturn(List.of());
        ExistingCoursePairProjection rejectedPair = mock(ExistingCoursePairProjection.class);
        when(rejectedPair.getMission2Id()).thenReturn(2L);
        when(rejectedPair.getMission3Id()).thenReturn(3L);
        when(courseMissionRepository.findExistingPairs(9L, 1L)).thenReturn(List.of(rejectedPair));
        CourseCandidateMissionProjection candidateM2First = candidate(2L, "37.5670", "126.9780");
        CourseCandidateMissionProjection candidateM2Second = candidate(4L, "37.5680", "126.9780");
        CourseCandidateMissionProjection candidateM3ForFirst = candidate(3L, "37.5715", "126.9850");
        CourseCandidateMissionProjection candidateM3ForSecond = candidate(3L, "37.5715", "126.9850");
        when(candidateRepository.findNearestCandidates(
                eq(anchor.getSnapshotLatitude()), eq(anchor.getSnapshotLongitude()), eq(500),
                any(), any(), eq(9L), anyInt()
        )).thenReturn(List.of(candidateM2First, candidateM2Second));
        when(candidateRepository.findNearestCandidates(
                eq(m2First.getSnapshotLatitude()), eq(m2First.getSnapshotLongitude()), eq(500),
                any(), any(), eq(9L), anyInt()
        )).thenReturn(List.of(candidateM3ForFirst));
        when(candidateRepository.findNearestCandidates(
                eq(m2Second.getSnapshotLatitude()), eq(m2Second.getSnapshotLongitude()), eq(500),
                any(), any(), eq(9L), anyInt()
        )).thenReturn(List.of(candidateM3ForSecond));
        when(missionCourseRepository.save(any(MissionCourse.class))).thenAnswer(inv -> {
            MissionCourse c = inv.getArgument(0);
            setCourseId(c, 200L);
            return c;
        });
        when(courseMissionRepository.save(any(CourseMission.class))).thenAnswer(inv -> inv.getArgument(0));

        MissionCourseDetailResultDTO result = service.generateAndStart(9L, 1L);

        // (2,3)은 기존 코스와 겹쳐서 제외되고, (4,3)이 다음 후보로 선택된다.
        assertThat(result.course().getName()).isEqualTo("경복궁 주변 코스");
        verify(courseMissionRepository, times(3)).save(any(CourseMission.class));
    }
}
