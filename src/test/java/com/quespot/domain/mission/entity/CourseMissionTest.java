package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CourseMissionTest {

    private Mission mission(String suffix) {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("cm-" + suffix)
                .name("테스트 장소 " + suffix)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        return Mission.publish(MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1));
    }

    @Test
    void ofBuildsCourseMissionWithGivenSeq() {
        Mission anchor = mission("a");
        MissionCourse course = MissionCourse.generate(
                anchor, 1L, 1, "코스", null, null, null, 100, 30, 30
        );

        CourseMission courseMission = CourseMission.of(course, anchor, 1);

        assertThat(courseMission.getCourse()).isSameAs(course);
        assertThat(courseMission.getMission()).isSameAs(anchor);
        assertThat(courseMission.getSeq()).isEqualTo(1);
    }
}
