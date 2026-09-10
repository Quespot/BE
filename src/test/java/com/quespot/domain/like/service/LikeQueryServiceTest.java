package com.quespot.domain.like.service;

import com.quespot.domain.like.dto.LikedCourseRowDTO;
import com.quespot.domain.like.dto.LikedMissionRowDTO;
import com.quespot.domain.like.dto.res.LikedCourseListResponseDTO;
import com.quespot.domain.like.dto.res.LikedMissionListResponseDTO;
import com.quespot.domain.like.repository.LikeRepository;
import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LikeQueryServiceTest {

    private LikeRepository likeRepository;
    private CourseAttemptRepository courseAttemptRepository;
    private LikeQueryService service;

    @BeforeEach
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        courseAttemptRepository = mock(CourseAttemptRepository.class);
        service = new LikeQueryService(likeRepository, courseAttemptRepository);
    }

    private Mission mission(Long id, String name) {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("1").name(name)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        Mission mission = Mission.publish(MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1));
        ReflectionTestUtils.setField(mission, "id", id);
        return mission;
    }

    private MissionCourse course(Long id, Long ownerId) {
        MissionCourse course = MissionCourse.generate(
                mission(100L, "anchor"), ownerId, 3, "서울 역사 탐방 코스", null, null, "11",
                820, 100, 90
        );
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    @Test
    void likedMissionsMapRowsInRepositoryOrderWithCount() {
        LocalDateTime t = LocalDateTime.of(2026, 9, 10, 12, 0);
        when(likeRepository.findLikedMissions(7L)).thenReturn(List.of(
                new LikedMissionRowDTO(mission(2L, "인사아트센터"), t),
                new LikedMissionRowDTO(mission(1L, "전통 찻집 다향"), t.minusMinutes(1))
        ));

        LikedMissionListResponseDTO result = service.getLikedMissions(7L);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.items()).extracting(i -> i.missionId()).containsExactly(2L, 1L);
        assertThat(result.items().get(0).spotName()).isEqualTo("인사아트센터");
        assertThat(result.items().get(0).likedAt()).isEqualTo(t);
        verifyNoInteractions(courseAttemptRepository);
    }

    @Test
    void likedCoursesCarryMyAttemptStatusAndIgnoreOtherUsersAttempts() {
        MissionCourse liked = course(5L, 7L);
        when(likeRepository.findLikedCourses(7L)).thenReturn(List.of(
                new LikedCourseRowDTO(liked, LocalDateTime.now())
        ));
        CourseAttempt mine = CourseAttempt.start(7L, liked);
        mine.complete(100);
        CourseAttempt someoneElses = CourseAttempt.start(99L, liked);
        when(courseAttemptRepository.findByCourseIdIn(List.of(5L))).thenReturn(List.of(someoneElses, mine));

        LikedCourseListResponseDTO result = service.getLikedCourses(7L);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items().get(0).courseId()).isEqualTo(5L);
        assertThat(result.items().get(0).myStatus()).isEqualTo(CourseAttemptStatus.COMPLETED);
        assertThat(result.items().get(0).totalRewardPoint()).isEqualTo(820);
    }

    @Test
    void likedCoursesWithNoRowsDoesNotQueryAttempts() {
        when(likeRepository.findLikedCourses(7L)).thenReturn(List.of());

        LikedCourseListResponseDTO result = service.getLikedCourses(7L);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
        verifyNoInteractions(courseAttemptRepository);
    }
}
