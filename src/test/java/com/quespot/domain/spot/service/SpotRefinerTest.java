package com.quespot.domain.spot.service;

import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpotRefinerTest {

    private TourContentRawRepository tourContentRawRepository;
    private SpotRefinementWriter spotRefinementWriter;
    private SpotRefiner spotRefiner;

    @BeforeEach
    void setUp() {
        tourContentRawRepository = mock(TourContentRawRepository.class);
        spotRefinementWriter = mock(SpotRefinementWriter.class);
        spotRefiner = new SpotRefiner(tourContentRawRepository, spotRefinementWriter);
    }

    private TourContentRaw rawOf(String contentId, LocalDateTime apiModifiedTime) {
        TourContentRaw raw = mock(TourContentRaw.class);
        when(raw.getContentId()).thenReturn(contentId);
        when(raw.getApiModifiedTime()).thenReturn(apiModifiedTime);
        return raw;
    }

    @Test
    void keepsOnlyLatestRawPerContentIdWhenGivenDescendingOrder() {
        TourContentRaw contentOneNewer = rawOf("1", LocalDateTime.of(2026, 2, 1, 0, 0));
        TourContentRaw contentOneOlder = rawOf("1", LocalDateTime.of(2026, 1, 1, 0, 0));
        TourContentRaw contentTwo = rawOf("2", LocalDateTime.of(2026, 1, 15, 0, 0));
        // findByOperationOrderByApiModifiedTimeDesc는 이미 내림차순으로 반환한다고 가정.
        when(tourContentRawRepository.findByOperationOrderByApiModifiedTimeDesc("areaBasedSyncList2"))
                .thenReturn(List.of(contentOneNewer, contentTwo, contentOneOlder));
        when(spotRefinementWriter.refineChunk(anyList()))
                .thenReturn(new RefinementSummary(0, 0, 0, 0, 0));

        spotRefiner.refine();

        ArgumentCaptor<List<TourContentRaw>> captor = ArgumentCaptor.forClass(List.class);
        verify(spotRefinementWriter).refineChunk(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(TourContentRaw::getContentId)
                .containsExactlyInAnyOrder("1", "2");
        assertThat(captor.getValue()).filteredOn(raw -> raw.getContentId().equals("1"))
                .extracting(TourContentRaw::getApiModifiedTime)
                .containsExactly(LocalDateTime.of(2026, 2, 1, 0, 0));
    }

    @Test
    void splitsIntoChunksOfTwentyAndAggregatesSummary() {
        List<TourContentRaw> allRaws = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            allRaws.add(rawOf(String.valueOf(i), LocalDateTime.of(2026, 1, 1, 0, 0)));
        }
        when(tourContentRawRepository.findByOperationOrderByApiModifiedTimeDesc("areaBasedSyncList2"))
                .thenReturn(allRaws);
        when(spotRefinementWriter.refineChunk(anyList()))
                .thenReturn(new RefinementSummary(1, 0, 0, 0, 0));

        RefinementSummary summary = spotRefiner.refine();

        // 45건 -> 20/20/5 세 청크
        verify(spotRefinementWriter, times(3)).refineChunk(anyList());
        assertThat(summary).isEqualTo(new RefinementSummary(3, 0, 0, 0, 0));
    }
}
