package com.quespot.domain.spot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import com.quespot.domain.tour.entity.TourContentRaw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpotRefinementWriterTest {

    private SpotRepository spotRepository;
    private CategoryResolver categoryResolver;
    private SpotRefinementWriter spotRefinementWriter;

    @BeforeEach
    void setUp() {
        spotRepository = mock(SpotRepository.class);
        categoryResolver = mock(CategoryResolver.class);
        when(categoryResolver.resolve(any(), any(), any())).thenReturn(AppCategory.CULTURE);
        when(categoryResolver.getCurrentVersion()).thenReturn(1);

        spotRefinementWriter = new SpotRefinementWriter(spotRepository, categoryResolver, new ObjectMapper());

        when(spotRepository.saveAndFlush(any(Spot.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TourContentRaw rawOf(String contentId, LocalDateTime apiModifiedTime, String payloadJson) {
        TourContentRaw raw = mock(TourContentRaw.class);
        when(raw.getContentId()).thenReturn(contentId);
        when(raw.getApiModifiedTime()).thenReturn(apiModifiedTime);
        when(raw.getPayload()).thenReturn(payloadJson);
        return raw;
    }

    private String payload(String contentId, String mapx, String mapy, String firstimage, String showflag) {
        return """
                {"contentid":"%s","contenttypeid":"12","title":"테스트 스팟",
                "addr1":"서울특별시 종로구","addr2":"1층","mapx":"%s","mapy":"%s",
                "firstimage":"%s","firstimage2":"thumb.jpg","showflag":"%s",
                "cpyrhtDivCd":"Type3","lDongRegnCd":"11","lDongSignguCd":"110",
                "lclsSystm1":"VE","lclsSystm2":"VE01","lclsSystm3":"VE010100"}
                """.formatted(contentId, mapx, mapy, firstimage, showflag);
    }

    @Test
    void createsNewSpotWhenNoneExists() {
        TourContentRaw raw = rawOf("1", LocalDateTime.of(2026, 1, 1, 0, 0),
                payload("1", "127.0", "37.5", "img.jpg", "1"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "1"))
                .thenReturn(Optional.empty());

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(raw));

        assertThat(summary).isEqualTo(new RefinementSummary(1, 0, 0, 0, 0));
        ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
        verify(spotRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("테스트 스팟");
        assertThat(captor.getValue().getAddress()).isEqualTo("서울특별시 종로구 1층");
        assertThat(captor.getValue().getShowFlag()).isTrue();
        assertThat(captor.getValue().getAppCategory()).isEqualTo(AppCategory.CULTURE);
        assertThat(captor.getValue().getCategoryMappingVersion()).isEqualTo(1);
    }

    @Test
    void updatesExistingSpotWhenSourceModifiedAtDiffers() {
        Spot existing = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("1").name("옛이름")
                .latitude(java.math.BigDecimal.ZERO).longitude(java.math.BigDecimal.ZERO)
                .appCategory(AppCategory.UNMAPPED).categoryMappingVersion(1).showFlag(true)
                .sourceModifiedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
        TourContentRaw raw = rawOf("1", LocalDateTime.of(2026, 1, 1, 0, 0),
                payload("1", "127.0", "37.5", "img.jpg", "1"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "1"))
                .thenReturn(Optional.of(existing));

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(raw));

        assertThat(summary).isEqualTo(new RefinementSummary(0, 1, 0, 0, 0));
        assertThat(existing.getName()).isEqualTo("테스트 스팟");
    }

    @Test
    void skipsWhenExistingSpotHasSameSourceModifiedAt() {
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        Spot existing = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("1").name("이름")
                .latitude(java.math.BigDecimal.ZERO).longitude(java.math.BigDecimal.ZERO)
                .appCategory(AppCategory.UNMAPPED).categoryMappingVersion(1).showFlag(true)
                .sourceModifiedAt(sameTime)
                .build();
        TourContentRaw raw = rawOf("1", sameTime, payload("1", "127.0", "37.5", "img.jpg", "1"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "1"))
                .thenReturn(Optional.of(existing));

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(raw));

        assertThat(summary).isEqualTo(new RefinementSummary(0, 0, 0, 1, 0));
        verify(spotRepository, never()).saveAndFlush(any());
    }

    @Test
    void skipsWhenCoordinatesAreBlank() {
        TourContentRaw raw = rawOf("1", LocalDateTime.of(2026, 1, 1, 0, 0),
                payload("1", "", "", "img.jpg", "1"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "1"))
                .thenReturn(Optional.empty());

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(raw));

        assertThat(summary).isEqualTo(new RefinementSummary(0, 0, 1, 0, 0));
        verify(spotRepository, never()).saveAndFlush(any());
    }

    @Test
    void skipsWhenCoordinatesAreNotNumeric() {
        TourContentRaw raw = rawOf("1", LocalDateTime.of(2026, 1, 1, 0, 0),
                payload("1", "not-a-number", "37.5", "img.jpg", "1"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "1"))
                .thenReturn(Optional.empty());

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(raw));

        assertThat(summary).isEqualTo(new RefinementSummary(0, 0, 1, 0, 0));
    }

    @Test
    void savesNormallyEvenWhenCoordinatesAreOutsideKoreaBbox() {
        // bbox 이탈은 스킵이 아니라 저장 + WARN 로그만. 0,0(기니만)으로 뒤바뀐 사례.
        TourContentRaw raw = rawOf("1", LocalDateTime.of(2026, 1, 1, 0, 0),
                payload("1", "0.0", "0.0", "img.jpg", "1"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "1"))
                .thenReturn(Optional.empty());

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(raw));

        assertThat(summary).isEqualTo(new RefinementSummary(1, 0, 0, 0, 0));
        ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
        verify(spotRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getLatitude()).isEqualByComparingTo("0.0");
        assertThat(captor.getValue().getLongitude()).isEqualByComparingTo("0.0");
    }

    @Test
    void oneMalformedItemDoesNotStopRemainingItemsInChunk() {
        TourContentRaw badRaw = mock(TourContentRaw.class);
        when(badRaw.getContentId()).thenReturn("bad");
        when(badRaw.getPayload()).thenReturn("not-json");
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "bad"))
                .thenReturn(Optional.empty());

        TourContentRaw goodRaw = rawOf("2", LocalDateTime.of(2026, 1, 1, 0, 0),
                payload("2", "127.0", "37.5", "img.jpg", "1"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "2"))
                .thenReturn(Optional.empty());

        RefinementSummary summary = spotRefinementWriter.refineChunk(List.of(badRaw, goodRaw));

        assertThat(summary).isEqualTo(new RefinementSummary(1, 0, 0, 0, 1));
    }

    @Test
    void showFlagFalseIsPreservedNotDeleted() {
        TourContentRaw raw = rawOf("1", LocalDateTime.of(2026, 1, 1, 0, 0),
                payload("1", "127.0", "37.5", "img.jpg", "0"));
        when(spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, "1"))
                .thenReturn(Optional.empty());

        spotRefinementWriter.refineChunk(List.of(raw));

        ArgumentCaptor<Spot> captor = ArgumentCaptor.forClass(Spot.class);
        verify(spotRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getShowFlag()).isFalse();
    }
}
