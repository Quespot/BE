package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.CourseMissionItemResultDTO;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.projection.CourseMissionLockRowProjection;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseLockPolicyTest {

    private CourseMissionRepository courseMissionRepository;
    private MissionAttemptRepository missionAttemptRepository;
    private CourseLockPolicy courseLockPolicy;

    @BeforeEach
    void setUp() {
        courseMissionRepository = mock(CourseMissionRepository.class);
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        courseLockPolicy = new CourseLockPolicy(courseMissionRepository, missionAttemptRepository);
    }

    private CourseMission courseMissionAt(int seq, Long missionId) {
        CourseMission cm = mock(CourseMission.class);
        Mission mission = mock(Mission.class);
        when(mission.getId()).thenReturn(missionId);
        when(cm.getMission()).thenReturn(mission);
        when(cm.getSeq()).thenReturn(seq);
        return cm;
    }

    @Test
    void resolveCourseMissionStatusesMarksSeq1AvailableAndSeq2LockedWhenSeq1NotDone() {
        CourseMission seq1 = courseMissionAt(1, 10L);
        CourseMission seq2 = courseMissionAt(2, 20L);

        List<CourseMissionItemResultDTO> result = courseLockPolicy.resolveCourseMissionStatuses(
                List.of(seq1, seq2), Set.of(), Set.of()
        );

        assertThat(result.get(0).status()).isEqualTo(UserMissionStatus.AVAILABLE);
        assertThat(result.get(1).status()).isEqualTo(UserMissionStatus.LOCKED);
    }

    @Test
    void resolveCourseMissionStatusesUnlocksSeq2WhenSeq1Completed() {
        CourseMission seq1 = courseMissionAt(1, 10L);
        CourseMission seq2 = courseMissionAt(2, 20L);

        List<CourseMissionItemResultDTO> result = courseLockPolicy.resolveCourseMissionStatuses(
                List.of(seq1, seq2), Set.of(10L), Set.of()
        );

        assertThat(result.get(0).status()).isEqualTo(UserMissionStatus.COMPLETED);
        assertThat(result.get(1).status()).isEqualTo(UserMissionStatus.AVAILABLE);
    }

    @Test
    void resolveCourseMissionStatusesReturnsInProgressWhenAttemptStarted() {
        CourseMission seq1 = courseMissionAt(1, 10L);

        List<CourseMissionItemResultDTO> result = courseLockPolicy.resolveCourseMissionStatuses(
                List.of(seq1), Set.of(), Set.of(10L)
        );

        assertThat(result.get(0).status()).isEqualTo(UserMissionStatus.IN_PROGRESS);
    }

    private CourseMissionLockRowProjection lockRow(Long missionId, Long prevMissionId) {
        CourseMissionLockRowProjection row = mock(CourseMissionLockRowProjection.class);
        when(row.getMissionId()).thenReturn(missionId);
        when(row.getPrevMissionId()).thenReturn(prevMissionId);
        return row;
    }

    @Test
    void resolveLockedMissionIdsReturnsEmptyWhenMissionNotInAnyInProgressCourse() {
        when(courseMissionRepository.findLockRowsForInProgressCourses(anyLong(), any()))
                .thenReturn(List.of());

        Set<Long> locked = courseLockPolicy.resolveLockedMissionIds(1L, List.of(20L));

        assertThat(locked).isEmpty();
    }

    @Test
    void resolveLockedMissionIdsLocksSeqGreaterThan1WhenPrevNotCompleted() {
        CourseMissionLockRowProjection row = lockRow(20L, 10L);
        when(courseMissionRepository.findLockRowsForInProgressCourses(1L, List.of(20L)))
                .thenReturn(List.of(row));
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(anyLong(), any(), any()))
                .thenReturn(List.of());

        Set<Long> locked = courseLockPolicy.resolveLockedMissionIds(1L, List.of(20L));

        assertThat(locked).containsExactly(20L);
    }

    @Test
    void resolveLockedMissionIdsUnlocksWhenPrevCompleted() {
        CourseMissionLockRowProjection row = lockRow(20L, 10L);
        when(courseMissionRepository.findLockRowsForInProgressCourses(1L, List.of(20L)))
                .thenReturn(List.of(row));
        MissionAttemptStatusProjection completedPrev = mock(MissionAttemptStatusProjection.class);
        when(completedPrev.getMissionId()).thenReturn(10L);
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(anyLong(), any(), any()))
                .thenReturn(List.of(completedPrev));

        Set<Long> locked = courseLockPolicy.resolveLockedMissionIds(1L, List.of(20L));

        assertThat(locked).isEmpty();
    }

    @Test
    void resolveLockedMissionIdsAllowsWhenUnlockedInAtLeastOneOfMultipleCourses() {
        CourseMissionLockRowProjection lockedInCourseA = lockRow(30L, 10L);
        CourseMissionLockRowProjection unlockedInCourseB = lockRow(30L, null);
        when(courseMissionRepository.findLockRowsForInProgressCourses(1L, List.of(30L)))
                .thenReturn(List.of(lockedInCourseA, unlockedInCourseB));
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(anyLong(), any(), any()))
                .thenReturn(List.of());

        Set<Long> locked = courseLockPolicy.resolveLockedMissionIds(1L, List.of(30L));

        assertThat(locked).isEmpty();
    }
}
