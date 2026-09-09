package com.quespot.domain.mission.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceNameFormatterTest {

    @Test
    void keepsShortNameUnchanged() {
        assertThat(PlaceNameFormatter.clean("경복궁")).isEqualTo("경복궁");
    }

    @Test
    void stripsTrailingParenthesesAnnotation() {
        assertThat(PlaceNameFormatter.clean("경복궁(고궁박물관)")).isEqualTo("경복궁");
    }

    @Test
    void truncatesLongNameWithEllipsis() {
        String longName = "아주아주아주아주아주긴장소이름입니다"; // 17자
        String result = PlaceNameFormatter.clean(longName);
        assertThat(result).isEqualTo(longName.substring(0, 12) + "…");
    }

    @Test
    void trimsWhitespace() {
        assertThat(PlaceNameFormatter.clean("  서울숲  ")).isEqualTo("서울숲");
    }

    @Test
    void fallsBackToOriginalWhenNameIsOnlyParentheses() {
        assertThat(PlaceNameFormatter.clean("(테스트)")).isEqualTo("(테스트)");
    }
}
