package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MissionAttemptTest {

    private Mission mission() {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("1").name("테스트 스팟")
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        MissionCandidate candidate = MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1);
        return Mission.publish(candidate);
    }

    @Test
    void startCreatesInProgressAttempt() {
        Mission mission = mission();

        MissionAttempt attempt = MissionAttempt.start(1L, mission);

        assertThat(attempt.getUserId()).isEqualTo(1L);
        assertThat(attempt.getMission()).isEqualTo(mission);
        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.IN_PROGRESS);
        assertThat(attempt.getStartedAt()).isNotNull();
        assertThat(attempt.getCompletedAt()).isNull();
        assertThat(attempt.getCourseAttemptId()).isNull();
    }

    @Test
    void completeSetsCompletedStatusAndFields() {
        MissionAttempt attempt = MissionAttempt.start(1L, mission());

        attempt.complete(new BigDecimal("37.5665"), new BigDecimal("126.9780"), 100);

        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.COMPLETED);
        assertThat(attempt.getArrivalLatitude()).isEqualByComparingTo("37.5665");
        assertThat(attempt.getArrivalLongitude()).isEqualByComparingTo("126.9780");
        assertThat(attempt.getEarnedPoint()).isEqualTo(100);
        assertThat(attempt.getCompletedAt()).isNotNull();
    }

    @Test
    void quitSetsQuitStatus() {
        MissionAttempt attempt = MissionAttempt.start(1L, mission());

        attempt.quit();

        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.QUIT);
    }

    @Test
    void writeReflectionSetsReflectionText() {
        MissionAttempt attempt = MissionAttempt.start(1L, mission());

        attempt.writeReflection("좋았어요");

        assertThat(attempt.getReflection()).isEqualTo("좋았어요");
    }
}
