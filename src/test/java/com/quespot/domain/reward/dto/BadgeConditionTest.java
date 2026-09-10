package com.quespot.domain.reward.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.reward.enums.AchievementMetric;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeConditionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesMetricScopeAndThreshold() {
        Optional<BadgeCondition> parsed = BadgeCondition.parse(objectMapper,
                "{\"metric\":\"MISSION_COMPLETED\",\"scope\":{\"regionCode\":\"11\"},\"threshold\":10}");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().metric()).isEqualTo(AchievementMetric.MISSION_COMPLETED);
        assertThat(parsed.get().regionCode()).isEqualTo("11");
        assertThat(parsed.get().threshold()).isEqualTo(10);
    }

    @Test
    void scopeIsOptional() {
        Optional<BadgeCondition> parsed = BadgeCondition.parse(objectMapper,
                "{\"metric\":\"COURSE_COMPLETED\",\"threshold\":5}");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().regionCode()).isNull();
    }

    @Test
    void returnsEmptyForUnknownMetricOrMalformedJson() {
        assertThat(BadgeCondition.parse(objectMapper,
                "{\"metric\":\"MISSION_COMPLETE_COUNT\",\"scope\":\"ALL\",\"threshold\":1}")).isEmpty();
        assertThat(BadgeCondition.parse(objectMapper, "not json")).isEmpty();
        assertThat(BadgeCondition.parse(objectMapper, "{\"metric\":\"COURSE_COMPLETED\"}")).isEmpty();
    }
}
