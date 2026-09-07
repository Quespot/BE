package com.quespot.domain.tour.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.tour.client.TourApiClient;
import com.quespot.domain.tour.client.TourApiException;
import com.quespot.domain.tour.client.dto.TourApiResponse;
import com.quespot.domain.tour.entity.SyncCheckpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourContentCollectorTest {

    private TourApiClient tourApiClient;
    private QuotaGuard quotaGuard;
    private RawPersister rawPersister;
    private CheckpointWriter checkpointWriter;
    private TourContentCollector collector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        tourApiClient = mock(TourApiClient.class);
        quotaGuard = mock(QuotaGuard.class);
        rawPersister = mock(RawPersister.class);
        checkpointWriter = mock(CheckpointWriter.class);
        collector = new TourContentCollector(tourApiClient, quotaGuard, rawPersister, checkpointWriter);
        objectMapper = new ObjectMapper();

        when(checkpointWriter.loadOrStart(anyString(), anyInt(), any()))
                .thenAnswer(invocation -> SyncCheckpoint.init(invocation.getArgument(0), invocation.getArgument(1)));
    }

    private TourApiResponse<JsonNode> pageOf(int count) throws Exception {
        List<JsonNode> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(objectMapper.readTree("{\"contentid\":\"" + i + "\"}"));
        }
        return new TourApiResponse<>(new TourApiResponse.Response<>(
                new TourApiResponse.Header("0000", "OK"),
                new TourApiResponse.Body<>(items, 100, 1, count)
        ));
    }

    @Test
    void stopsImmediatelyWhenQuotaAlreadyExhausted() {
        when(quotaGuard.tryConsume()).thenReturn(false);

        collector.collect();

        verify(tourApiClient, never()).fetchSyncList(anyString(), anyInt(), anyInt(), anyInt(), any());
        verify(checkpointWriter, times(4)).suspendByQuota(anyString(), anyInt());
        verify(checkpointWriter, never()).complete(anyString(), anyInt());
    }

    @Test
    void suspendsWhenClientThrowsQuotaExceededException() {
        when(quotaGuard.tryConsume()).thenReturn(true);
        TourApiException quotaExceeded = TourApiException.fromXml(
                "<OpenAPI_ServiceResponse><cmmMsgHeader><returnReasonCode>22</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>");
        when(tourApiClient.fetchSyncList(anyString(), anyInt(), anyInt(), anyInt(), any()))
                .thenThrow(quotaExceeded);

        collector.collect();

        verify(checkpointWriter, times(4)).suspendByQuota(anyString(), anyInt());
        verify(checkpointWriter, never()).complete(anyString(), anyInt());
    }

    @Test
    void nonQuotaTourApiExceptionMarksCheckpointFailedAndContinuesOtherCombinations() {
        when(quotaGuard.tryConsume()).thenReturn(true);
        TourApiException applicationError = TourApiException.fromResultCode("0001", "APPLICATION_ERROR");
        when(tourApiClient.fetchSyncList(anyString(), anyInt(), anyInt(), anyInt(), any()))
                .thenThrow(applicationError);

        collector.collect();

        verify(checkpointWriter, times(4)).fail(anyString(), anyInt());
    }

    @Test
    void breaksOnEmptyPageThenCompletes() throws Exception {
        when(quotaGuard.tryConsume()).thenReturn(true);
        when(tourApiClient.fetchSyncList(anyString(), anyInt(), eq(1), anyInt(), any()))
                .thenReturn(pageOf(0));

        collector.collect();

        verify(rawPersister, never()).saveAll(anyString(), any());
        verify(checkpointWriter, times(4)).complete(anyString(), anyInt());
    }

    @Test
    void breaksOnPartialPageAfterPersistingAndAdvancing() throws Exception {
        when(quotaGuard.tryConsume()).thenReturn(true);
        when(tourApiClient.fetchSyncList(anyString(), anyInt(), eq(1), anyInt(), any()))
                .thenReturn(pageOf(40));

        collector.collect();

        verify(rawPersister, times(4)).saveAll(anyString(), any());
        verify(checkpointWriter, times(4)).advance(anyString(), anyInt(), eq(1));
        verify(checkpointWriter, times(4)).complete(anyString(), anyInt());
    }

    @Test
    void continuesToNextPageWhenFullPageReturnedThenBreaksOnPartialPage() throws Exception {
        when(quotaGuard.tryConsume()).thenReturn(true);
        when(tourApiClient.fetchSyncList(anyString(), anyInt(), eq(1), eq(100), any()))
                .thenReturn(pageOf(100));
        when(tourApiClient.fetchSyncList(anyString(), anyInt(), eq(2), eq(100), any()))
                .thenReturn(pageOf(30));

        collector.collect();

        verify(checkpointWriter, times(4)).advance(anyString(), anyInt(), eq(1));
        verify(checkpointWriter, times(4)).advance(anyString(), anyInt(), eq(2));
        verify(checkpointWriter, times(4)).complete(anyString(), anyInt());
    }
}
