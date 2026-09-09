package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionAttemptServiceTest {

    private MissionAttemptRepository missionAttemptRepository;
    private MissionRepository missionRepository;
    private CourseAttemptRepository courseAttemptRepository;
    private CourseMissionRepository courseMissionRepository;
    private MissionAttemptService missionAttemptService;

    @BeforeEach
    void setUp() {
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        missionRepository = mock(MissionRepository.class);
        courseAttemptRepository = mock(CourseAttemptRepository.class);
        courseMissionRepository = mock(CourseMissionRepository.class);
        missionAttemptService = new MissionAttemptService(
                missionAttemptRepository, missionRepository, courseAttemptRepository, courseMissionRepository
        );
    }

    private Mission activeMission() {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("1").name("테스트 스팟")
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        MissionCandidate candidate = MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1);
        return Mission.publish(candidate);
    }

    @Test
    void startCreatesNewAttemptWhenNoneExists() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(missionAttemptRepository.save(any(MissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MissionAttempt attempt = missionAttemptService.start(1L, 10L);

        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.IN_PROGRESS);
        verify(missionAttemptRepository).save(any(MissionAttempt.class));
    }

    @Test
    void startReturnsExistingAttemptWhenAlreadyInProgress() {
        Mission mission = activeMission();
        MissionAttempt existing = MissionAttempt.start(1L, mission);
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existing));

        MissionAttempt attempt = missionAttemptService.start(1L, 10L);

        assertThat(attempt).isSameAs(existing);
        verify(missionAttemptRepository, never()).save(any(MissionAttempt.class));
    }

    @Test
    void startThrowsWhenAlreadyCompleted() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.COMPLETED))
                .thenReturn(Optional.of(MissionAttempt.start(1L, mission)));

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_ALREADY_COMPLETED);
    }

    private CourseAttempt courseAttemptOwnedBy(Long userId, MissionCourse course) {
        CourseAttempt courseAttempt = mock(CourseAttempt.class);
        when(courseAttempt.getUserId()).thenReturn(userId);
        when(courseAttempt.getStatus()).thenReturn(CourseAttemptStatus.IN_PROGRESS);
        when(courseAttempt.getCourse()).thenReturn(course);
        return courseAttempt;
    }

    @Test
    void startWithCourseAttemptIdSucceedsWhenMissionBelongsToCourse() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.COMPLETED))
                .thenReturn(Optional.empty());
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        CourseAttempt courseAttempt = courseAttemptOwnedBy(1L, course);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(courseAttempt));
        when(courseMissionRepository.existsByCourseIdAndMissionId(100L, 10L)).thenReturn(true);
        when(missionAttemptRepository.save(any(MissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MissionAttempt attempt = missionAttemptService.start(1L, 10L, 55L);

        assertThat(attempt.getCourseAttemptId()).isEqualTo(55L);
    }

    @Test
    void startWithCourseAttemptIdThrowsWhenMissionNotInCourse() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        CourseAttempt courseAttempt = courseAttemptOwnedBy(1L, course);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(courseAttempt));
        when(courseMissionRepository.existsByCourseIdAndMissionId(100L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L, 55L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_NOT_IN_COURSE);
    }

    @Test
    void startWithCourseAttemptIdThrowsWhenCourseAttemptNotOwnedByUser() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        CourseAttempt courseAttempt = courseAttemptOwnedBy(2L, course);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(courseAttempt));

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L, 55L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.COURSE_ATTEMPT_NOT_FOUND);
    }

    @Test
    void startWithCourseAttemptIdThrowsWhenCourseAttemptNotInProgress() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        CourseAttempt courseAttempt = mock(CourseAttempt.class);
        when(courseAttempt.getUserId()).thenReturn(1L);
        when(courseAttempt.getStatus()).thenReturn(CourseAttemptStatus.COMPLETED);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(courseAttempt));

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L, 55L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.COURSE_ATTEMPT_NOT_IN_PROGRESS);
    }

    @Test
    void startAttachesCourseAttemptIdToExistingStandaloneInProgressAttempt() {
        Mission mission = activeMission();
        MissionAttempt existing = MissionAttempt.start(1L, mission);
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existing));
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        CourseAttempt courseAttempt = courseAttemptOwnedBy(1L, course);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(courseAttempt));
        when(courseMissionRepository.existsByCourseIdAndMissionId(100L, 10L)).thenReturn(true);

        MissionAttempt attempt = missionAttemptService.start(1L, 10L, 55L);

        assertThat(attempt).isSameAs(existing);
        assertThat(attempt.getCourseAttemptId()).isEqualTo(55L);
        verify(missionAttemptRepository, never()).save(any(MissionAttempt.class));
    }

    @Test
    void startThrowsWhenExistingAttemptAlreadyLinkedToDifferentCourse() {
        Mission mission = activeMission();
        MissionAttempt existing = MissionAttempt.start(1L, mission, 99L);
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existing));
        MissionCourse course = mock(MissionCourse.class);
        when(course.getId()).thenReturn(100L);
        CourseAttempt courseAttempt = courseAttemptOwnedBy(1L, course);
        when(courseAttemptRepository.findById(55L)).thenReturn(Optional.of(courseAttempt));
        when(courseMissionRepository.existsByCourseIdAndMissionId(100L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L, 55L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_NOT_IN_COURSE);
    }

    @Test
    void startThrowsWhenMissionNotActive() {
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_NOT_FOUND);
    }

    @Test
    void quitSetsStatusToQuitWhenInProgress() {
        Mission mission = activeMission();
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        missionAttemptService.quit(1L, 100L);

        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.QUIT);
    }

    @Test
    void quitThrowsWhenNotInProgress() {
        Mission mission = activeMission();
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        attempt.quit();
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionAttemptService.quit(1L, 100L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.ATTEMPT_NOT_IN_PROGRESS);
    }

    @Test
    void getAttemptThrowsWhenNotOwnedByUser() {
        Mission mission = activeMission();
        MissionAttempt attempt = MissionAttempt.start(2L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> missionAttemptService.getAttempt(1L, 100L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.ATTEMPT_NOT_FOUND);
    }

    @Test
    void writeReflectionSetsReflectionOnOwnedAttempt() {
        Mission mission = activeMission();
        MissionAttempt attempt = MissionAttempt.start(1L, mission);
        when(missionAttemptRepository.findById(100L)).thenReturn(Optional.of(attempt));

        missionAttemptService.writeReflection(1L, 100L, "좋았어요");

        assertThat(attempt.getReflection()).isEqualTo("좋았어요");
    }
}
