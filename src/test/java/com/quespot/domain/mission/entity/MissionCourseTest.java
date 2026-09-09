package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MissionCourseTest {

    private Mission mission(String suffix) {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("mc-" + suffix)
                .name("테스트 장소 " + suffix)
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        MissionCandidate candidate = MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1);
        return Mission.publish(candidate);
    }

    @Test
    void generateBuildsCourseWithGivenFieldsAndAlwaysActiveNonPublic() {
        Mission anchor = mission("a");

        MissionCourse course = MissionCourse.generate(
                anchor, 1L, 3, "경복궁 주변 코스", "설명입니다", "https://img", "11110",
                300, 90, 100
        );

        assertThat(course.getAnchorMission()).isSameAs(anchor);
        assertThat(course.getCreatedByUserId()).isEqualTo(1L);
        assertThat(course.getMissionCount()).isEqualTo(3);
        assertThat(course.getName()).isEqualTo("경복궁 주변 코스");
        assertThat(course.getDescription()).isEqualTo("설명입니다");
        assertThat(course.getCoverImageUrl()).isEqualTo("https://img");
        assertThat(course.getRegionCode()).isEqualTo("11110");
        assertThat(course.getTotalRewardPoint()).isEqualTo(300);
        assertThat(course.getBonusPoint()).isEqualTo(90);
        assertThat(course.getEstimatedMinutes()).isEqualTo(100);
        assertThat(course.getStatus()).isEqualTo(MissionCourseStatus.ACTIVE);
        assertThat(course.getIsPublic()).isFalse();
    }
}
