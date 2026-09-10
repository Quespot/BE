package com.quespot.domain.like.service;

import com.quespot.domain.like.enums.LikeTargetType;
import com.quespot.domain.like.exception.LikeException;
import com.quespot.domain.like.exception.code.LikeErrorCode;
import com.quespot.domain.like.repository.LikeRepository;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.service.CourseLockPolicy;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LikeServiceTest {

    private LikeRepository likeRepository;
    private MissionRepository missionRepository;
    private SpotRepository spotRepository;
    private MissionCourseRepository missionCourseRepository;
    private CourseLockPolicy courseLockPolicy;
    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        missionRepository = mock(MissionRepository.class);
        spotRepository = mock(SpotRepository.class);
        missionCourseRepository = mock(MissionCourseRepository.class);
        courseLockPolicy = mock(CourseLockPolicy.class);
        likeService = new LikeService(
                likeRepository, missionRepository, spotRepository, missionCourseRepository, courseLockPolicy
        );
    }

    @Test
    void likeMissionUpsertsWhenMissionIsActiveAndUnlocked() {
        when(missionRepository.findByIdAndStatus(1L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mock(Mission.class)));
        when(courseLockPolicy.resolveLockedMissionIds(eq(7L), anyList())).thenReturn(Set.of());

        likeService.likeMission(7L, 1L);

        verify(likeRepository).upsert(7L, "MISSION", 1L);
    }

    @Test
    void likeMissionThrowsWhenMissionNotActive() {
        when(missionRepository.findByIdAndStatus(1L, MissionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.likeMission(7L, 1L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_NOT_FOUND);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void likeMissionThrowsWhenMissionLockedForUser() {
        when(missionRepository.findByIdAndStatus(1L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mock(Mission.class)));
        when(courseLockPolicy.resolveLockedMissionIds(7L, List.of(1L))).thenReturn(Set.of(1L));

        assertThatThrownBy(() -> likeService.likeMission(7L, 1L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_LOCKED);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void likeSpotThrowsWhenSpotHiddenByShowFlag() {
        Spot hidden = mock(Spot.class);
        when(hidden.getShowFlag()).thenReturn(false);
        when(spotRepository.findById(3L)).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> likeService.likeSpot(7L, 3L))
                .isInstanceOf(LikeException.class)
                .extracting(e -> ((LikeException) e).getErrorCode())
                .isEqualTo(LikeErrorCode.SPOT_NOT_FOUND);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void likeSpotUpsertsWhenSpotVisible() {
        Spot visible = mock(Spot.class);
        when(visible.getShowFlag()).thenReturn(true);
        when(spotRepository.findById(3L)).thenReturn(Optional.of(visible));

        likeService.likeSpot(7L, 3L);

        verify(likeRepository).upsert(7L, "SPOT", 3L);
    }

    @Test
    void likeCourseThrowsWhenCourseOwnedByAnotherUser() {
        MissionCourse course = mock(MissionCourse.class);
        when(course.getCreatedByUserId()).thenReturn(99L);
        when(missionCourseRepository.findByIdAndStatus(5L, MissionCourseStatus.ACTIVE)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> likeService.likeCourse(7L, 5L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.COURSE_NOT_FOUND);
        verifyNoInteractions(likeRepository);
    }

    @Test
    void likeCourseUpsertsWhenOwnedByUser() {
        MissionCourse course = mock(MissionCourse.class);
        when(course.getCreatedByUserId()).thenReturn(7L);
        when(missionCourseRepository.findByIdAndStatus(5L, MissionCourseStatus.ACTIVE)).thenReturn(Optional.of(course));

        likeService.likeCourse(7L, 5L);

        verify(likeRepository).upsert(7L, "COURSE", 5L);
    }

    @Test
    void unlikeDeletesWithoutAnyTargetValidation() {
        likeService.unlike(7L, LikeTargetType.MISSION, 1L);

        verify(likeRepository).deleteByUserIdAndTargetTypeAndTargetId(7L, LikeTargetType.MISSION, 1L);
        verifyNoInteractions(missionRepository, spotRepository, missionCourseRepository, courseLockPolicy);
    }
}
