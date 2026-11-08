package com.quespot.domain.spot.service;

import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpotRefinerTest {

    private static final String OPERATION = "areaBasedSyncList2";

    private TourContentRawRepository tourContentRawRepository;
    private SpotRefinementWriter spotRefinementWriter;
    private SpotRefiner spotRefiner;

    @BeforeEach
    void setUp() {
        tourContentRawRepository = mock(TourContentRawRepository.class);
        spotRefinementWriter = mock(SpotRefinementWriter.class);
        CategoryResolver categoryResolver = mock(CategoryResolver.class);
        when(categoryResolver.getCurrentVersion()).thenReturn(1);
        spotRefiner = new SpotRefiner(tourContentRawRepository, spotRefinementWriter, categoryResolver);
    }

    @Test
    void refinesOnlySelectedTargetsInChunksOfTwoHundred() {
        List<Long> targetIds = new ArrayList<>();
        for (long id = 1; id <= 450; id++) {
            targetIds.add(id);
        }
        when(tourContentRawRepository.findRefinementTargetIds(OPERATION, 1)).thenReturn(targetIds);
        when(tourContentRawRepository.countContents(OPERATION)).thenReturn(500L);
        when(tourContentRawRepository.findAllById(anyList())).thenReturn(List.of(mock(TourContentRaw.class)));
        when(spotRefinementWriter.refineChunk(anyList())).thenReturn(new RefinementSummary(1, 0, 0, 0, 0));

        RefinementSummary summary = spotRefiner.refine();

        // 450건 -> 200/200/50 세 청크, 대상에서 빠진 50건은 변경 없음으로 센다
        verify(tourContentRawRepository, times(3)).findAllById(anyList());
        verify(spotRefinementWriter, times(3)).refineChunk(anyList());
        assertThat(summary).isEqualTo(new RefinementSummary(3, 0, 0, 50, 0));
    }

    @Test
    void doesNotLoadRawPayloadWhenNothingChanged() {
        when(tourContentRawRepository.findRefinementTargetIds(OPERATION, 1)).thenReturn(List.of());
        when(tourContentRawRepository.countContents(OPERATION)).thenReturn(8000L);

        RefinementSummary summary = spotRefiner.refine();

        verify(tourContentRawRepository, never()).findAllById(anyList());
        verify(spotRefinementWriter, never()).refineChunk(anyList());
        assertThat(summary).isEqualTo(new RefinementSummary(0, 0, 0, 8000, 0));
    }
}
