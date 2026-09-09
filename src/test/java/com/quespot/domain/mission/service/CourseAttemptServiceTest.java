package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import com.quespot.domain.reward.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseAttemptServiceTest {

    private CourseAttemptRepository courseAttemptRepository;
    private MissionCourseRepository missionCourseRepository;
    private MissionAttemptRepository missionAttemptRepository;
    private CourseMissionRepository courseMissionRepository;
    private PointService pointService;
    private CourseAttemptService courseAttemptService;

    @BeforeEach
    void setUp() {
        courseAttemptRepository = mock(CourseAttemptRepository.class);
        missionCourseRepository = mock(MissionCourseRepository.class);
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        courseMissionRepository = mock(CourseMissionRepository.class);
        pointService = mock(PointService.class);
        courseAttemptService = new CourseAttemptService(
                courseAttemptRepository, missionCourseRepository, missionAttemptRepository,
                courseMissionRepository, pointService
        );
    }

    private MissionCourse activeCourse() {
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        when(course.getStatus()).thenReturn(MissionCourseStatus.ACTIVE);
        when(course.getBonusPoint()).thenReturn(50);
        when(course.getName()).thenReturn("정동 도보 코스");
        return course;
    }

    @Test
    void startCreatesNewCourseAttemptWhenNoneExists() {
        MissionCourse course = activeCourse();
        when(missionCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(courseAttemptRepository.save(any(CourseAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseAttempt attempt = courseAttemptService.start(1L, 100L);

        assertThat(attempt.getStatus()).isEqualTo(CourseAttemptStatus.IN_PROGRESS);
    }

    @Test
    void startReturnsExistingAttemptWhenAlreadyInProgress() {
        MissionCourse course = activeCourse();
        CourseAttempt existing = CourseAttempt.start(1L, course);
        when(missionCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existing));

        CourseAttempt attempt = courseAttemptService.start(1L, 100L);

        assertThat(attempt).isSameAs(existing);
        verify(courseAttemptRepository, never()).save(any(CourseAttempt.class));
    }

    @Test
    void startThrowsWhenAlreadyCompleted() {
        MissionCourse course = activeCourse();
        when(missionCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(courseAttemptRepository.findByUserIdAndCourseIdAndStatus(1L, 100L, CourseAttemptStatus.COMPLETED))
                .thenReturn(Optional.of(CourseAttempt.start(1L, course)));

        assertThatThrownBy(() -> courseAttemptService.start(1L, 100L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.COURSE_ALREADY_COMPLETED);
    }

    @Test
    void quitSetsStatusToQuitAndClearsCourseAttemptIdOnMissionAttempts() {
        MissionCourse course = activeCourse();
        CourseAttempt attempt = CourseAttempt.start(1L, course);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(attempt));
        MissionAttempt missionAttempt1 = mock(MissionAttempt.class);
        MissionAttempt missionAttempt2 = mock(MissionAttempt.class);
        when(missionAttemptRepository.findByCourseAttemptId(55L))
                .thenReturn(List.of(missionAttempt1, missionAttempt2));

        courseAttemptService.quit(1L, 55L);

        assertThat(attempt.getStatus()).isEqualTo(CourseAttemptStatus.QUIT);
        verify(missionAttempt1).clearCourseAttempt();
        verify(missionAttempt2).clearCourseAttempt();
    }

    @Test
    void quitThrowsWhenNotOwnedByUser() {
        MissionCourse course = activeCourse();
        CourseAttempt attempt = CourseAttempt.start(2L, course);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> courseAttemptService.quit(1L, 55L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.COURSE_ATTEMPT_NOT_FOUND);
    }

    @Test
    void quitThrowsWhenNotInProgress() {
        MissionCourse course = activeCourse();
        CourseAttempt attempt = CourseAttempt.start(1L, course);
        attempt.quit();
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> courseAttemptService.quit(1L, 55L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.COURSE_ATTEMPT_NOT_IN_PROGRESS);
    }

    @Test
    void tryCompleteCompletesCourseAndCreditsBonusWhenAllMissionsCompleted() {
        MissionCourse course = activeCourse();
        CourseAttempt attempt = CourseAttempt.start(1L, course);
        setId(attempt, 55L);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(attempt));
        when(courseMissionRepository.findMissionIdsByCourseId(100L)).thenReturn(List.of(10L, 20L, 30L));
        MissionAttemptStatusProjection p10 = statusProjection(10L);
        MissionAttemptStatusProjection p20 = statusProjection(20L);
        MissionAttemptStatusProjection p30 = statusProjection(30L);
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(
                eq(1L), eq(List.of(10L, 20L, 30L)), eq(List.of(MissionAttemptStatus.COMPLETED))
        )).thenReturn(List.of(p10, p20, p30));

        courseAttemptService.tryCompleteViaMissionCompletion(55L);

        assertThat(attempt.getStatus()).isEqualTo(CourseAttemptStatus.COMPLETED);
        assertThat(attempt.getEarnedBonusPoint()).isEqualTo(50);
        verify(pointService).credit(1L, 50, "COURSE_BONUS", "COURSE_ATTEMPT", 55L, "정동 도보 코스 완주 보너스");
    }

    @Test
    void tryCompleteDoesNothingWhenNotAllMissionsCompleted() {
        MissionCourse course = activeCourse();
        CourseAttempt attempt = CourseAttempt.start(1L, course);
        setId(attempt, 55L);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(attempt));
        when(courseMissionRepository.findMissionIdsByCourseId(100L)).thenReturn(List.of(10L, 20L, 30L));
        MissionAttemptStatusProjection p10 = statusProjection(10L);
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(
                eq(1L), eq(List.of(10L, 20L, 30L)), eq(List.of(MissionAttemptStatus.COMPLETED))
        )).thenReturn(List.of(p10));

        courseAttemptService.tryCompleteViaMissionCompletion(55L);

        assertThat(attempt.getStatus()).isEqualTo(CourseAttemptStatus.IN_PROGRESS);
        verify(pointService, never()).credit(anyLong(), anyInt(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void tryCompleteIsNoOpWhenCourseAttemptAlreadyCompleted() {
        MissionCourse course = activeCourse();
        CourseAttempt attempt = CourseAttempt.start(1L, course);
        attempt.complete(50);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(attempt));

        courseAttemptService.tryCompleteViaMissionCompletion(55L);

        verify(courseMissionRepository, never()).findMissionIdsByCourseId(any());
        verify(pointService, never()).credit(anyLong(), anyInt(), anyString(), anyString(), any(), anyString());
    }

    private MissionAttemptStatusProjection statusProjection(Long missionId) {
        MissionAttemptStatusProjection projection = mock(MissionAttemptStatusProjection.class);
        when(projection.getMissionId()).thenReturn(missionId);
        when(projection.getStatus()).thenReturn(MissionAttemptStatus.COMPLETED);
        return projection;
    }

    private void setId(CourseAttempt attempt, Long id) {
        try {
            Field field = CourseAttempt.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(attempt, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
