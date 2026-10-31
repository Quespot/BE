package com.quespot.domain.spot.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefinementSummaryTest {

    @Test
    void combineSumsEachFieldAcrossChunks() {
        RefinementSummary chunk1 = new RefinementSummary(3, 1, 0, 2, 0);
        RefinementSummary chunk2 = new RefinementSummary(5, 0, 1, 0, 1);

        RefinementSummary combined = RefinementSummary.combine(List.of(chunk1, chunk2));

        assertThat(combined).isEqualTo(new RefinementSummary(8, 1, 1, 2, 1));
    }

    @Test
    void combineOfEmptyListIsAllZero() {
        RefinementSummary combined = RefinementSummary.combine(List.of());

        assertThat(combined).isEqualTo(new RefinementSummary(0, 0, 0, 0, 0));
    }
}
