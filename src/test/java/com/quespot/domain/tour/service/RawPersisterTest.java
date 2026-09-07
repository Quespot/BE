package com.quespot.domain.tour.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RawPersisterTest {

    private TourContentRawRepository tourContentRawRepository;
    private RawPersister rawPersister;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        tourContentRawRepository = mock(TourContentRawRepository.class);
        objectMapper = new ObjectMapper();
        rawPersister = new RawPersister(new RawContentRowWriter(tourContentRawRepository, objectMapper));
    }

    private JsonNode item(String contentId) throws Exception {
        return objectMapper.readTree("""
                {"contentid":"%s","modifiedtime":"20260906120000","showflag":"1"}
                """.formatted(contentId));
    }

    @Test
    void savesEachItemIndividually() throws Exception {
        List<JsonNode> items = List.of(item("1"), item("2"), item("3"));

        rawPersister.saveAll("areaBasedSyncList2", items);

        verify(tourContentRawRepository, times(3)).saveAndFlush(any(TourContentRaw.class));
    }

    @Test
    void oneDuplicateDoesNotStopRemainingItems() throws Exception {
        List<JsonNode> items = List.of(item("1"), item("2"), item("3"));
        when(tourContentRawRepository.saveAndFlush(any(TourContentRaw.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        rawPersister.saveAll("areaBasedSyncList2", items);

        verify(tourContentRawRepository, times(3)).saveAndFlush(any(TourContentRaw.class));
    }

    @Test
    void malformedModifiedTimeDoesNotStopRemainingItems() throws Exception {
        JsonNode badItem = objectMapper.readTree("""
                {"contentid":"1","modifiedtime":"not-a-date","showflag":"1"}
                """);
        JsonNode missingModifiedTime = objectMapper.readTree("""
                {"contentid":"2","showflag":"1"}
                """);
        JsonNode goodItem = item("3");

        rawPersister.saveAll("areaBasedSyncList2", List.of(badItem, missingModifiedTime, goodItem));

        verify(tourContentRawRepository, times(1)).saveAndFlush(any(TourContentRaw.class));
    }

    @Test
    void storesOriginalJsonNodeTextAsPayloadIncludingUndeclaredFields() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {"contentid":"1","modifiedtime":"20260906120000","showflag":"1","unknownField":"kept"}
                """);
        ArgumentCaptor<TourContentRaw> captor = ArgumentCaptor.forClass(TourContentRaw.class);

        rawPersister.saveAll("areaBasedSyncList2", List.of(node));

        verify(tourContentRawRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("unknownField");
    }
}
