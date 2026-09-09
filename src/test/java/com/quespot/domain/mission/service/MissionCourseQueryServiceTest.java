package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.MissionCourseDetailResultDTO;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionCourseQueryServiceTest {

    private MissionCourseRepository missionCourseRepository;
    private CourseMissionRepository courseMissionRepository;
    private MissionAttemptRepository missionAttemptRepository;
    private CourseAttemptRepository courseAttemptRepository;
    private MissionCourseQueryService service;

    @BeforeEach
    void setUp() {
        missionCourseRepository = mock(MissionCourseRepository.class);
        courseMissionRepository = mock(CourseMissionRepository.class);
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        courseAttemptRepository = mock(CourseAttemptRepository.class);
        service = new MissionCourseQueryService(
                missionCourseRepository, courseMissionRepository, missionAttemptRepository, courseAttemptRepository
        );
    }

    @Test
    void getCourseDetailMarksOnlyCompletedMissionsAndCallsBulkQueryOnce() {
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        when(missionCourseRepository.findById(100L)).thenReturn(Optional.of(course));

        CourseMission cm1 = mock(CourseMission.class);
        Mission mission1 = mock(Mission.class);
        when(mission1.getId()).thenReturn(10L);
        when(cm1.getMission()).thenReturn(mission1);
        CourseMission cm2 = mock(CourseMission.class);
        Mission mission2 = mock(Mission.class);
        when(mission2.getId()).thenReturn(20L);
        when(cm2.getMission()).thenReturn(mission2);
        when(courseMissionRepository.findByCourseIdOrderBySeq(100L)).thenReturn(List.of(cm1, cm2));

        MissionAttemptStatusProjection completedProjection = mock(MissionAttemptStatusProjection.class);
        when(completedProjection.getMissionId()).thenReturn(10L);
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(anyLong(), any(), any()))
                .thenReturn(List.of(completedProjection));
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.COMPLETED))
                .thenReturn(Optional.empty());

        MissionCourseDetailResultDTO result = service.getCourseDetail(1L, 100L);

        assertThat(result.missions()).hasSize(2);
        assertThat(result.missions().get(0).status()).isEqualTo(com.quespot.domain.mission.enums.UserMissionStatus.COMPLETED);
        assertThat(result.missions().get(1).status()).isEqualTo(com.quespot.domain.mission.enums.UserMissionStatus.AVAILABLE);
        assertThat(result.myStatus()).isNull();
        verify(missionAttemptRepository, times(1))
                .findByUserIdAndMissionIdInAndStatusIn(anyLong(), any(), any());
    }

    @Test
    void getCourseDetailReturnsInProgressStatusWhenUserHasInProgressAttempt() {
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        when(missionCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseMissionRepository.findByCourseIdOrderBySeq(100L)).thenReturn(List.of());
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.COMPLETED))
                .thenReturn(Optional.empty());
        var inProgressAttempt = mock(com.quespot.domain.mission.entity.CourseAttempt.class);
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(inProgressAttempt));

        MissionCourseDetailResultDTO result = service.getCourseDetail(1L, 100L);

        assertThat(result.myStatus()).isEqualTo(CourseAttemptStatus.IN_PROGRESS);
    }
}
