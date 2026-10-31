package com.quespot.domain.tour.service;

import com.quespot.domain.tour.entity.SyncCheckpoint;
import com.quespot.domain.tour.entity.SyncCheckpointId;
import com.quespot.domain.tour.enums.SyncStatus;
import com.quespot.domain.tour.repository.SyncCheckpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckpointWriterTest {

    private SyncCheckpointRepository syncCheckpointRepository;
    private CheckpointWriter checkpointWriter;

    @BeforeEach
    void setUp() {
        syncCheckpointRepository = mock(SyncCheckpointRepository.class);
        checkpointWriter = new CheckpointWriter(syncCheckpointRepository);
        when(syncCheckpointRepository.save(any(SyncCheckpoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void loadOrStartCreatesFreshCheckpointWhenNoneExists() {
        when(syncCheckpointRepository.findById(new SyncCheckpointId("11", 12)))
                .thenReturn(Optional.empty());

        SyncCheckpoint checkpoint = checkpointWriter.loadOrStart("11", 12, LocalDateTime.now());

        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.RUNNING);
        assertThat(checkpoint.nextPageNo()).isEqualTo(1);
        assertThat(checkpoint.getLastModifiedTime()).isNull();
    }

    @Test
    void loadOrStartSwitchesCompletedCheckpointToIncrementalMode() {
        SyncCheckpoint completed = SyncCheckpoint.init("11", 12);
        completed.complete();
        when(syncCheckpointRepository.findById(new SyncCheckpointId("11", 12)))
                .thenReturn(Optional.of(completed));
        LocalDateTime yesterday = LocalDateTime.of(2026, 9, 6, 0, 0);

        SyncCheckpoint checkpoint = checkpointWriter.loadOrStart("11", 12, yesterday);

        assertThat(checkpoint.nextPageNo()).isEqualTo(1);
        assertThat(checkpoint.getLastModifiedTime()).isEqualTo(yesterday);
        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.RUNNING);
    }

    @Test
    void loadOrStartLeavesInProgressCheckpointUntouched() {
        SyncCheckpoint inProgress = SyncCheckpoint.init("11", 12);
        inProgress.advance(3);
        when(syncCheckpointRepository.findById(new SyncCheckpointId("11", 12)))
                .thenReturn(Optional.of(inProgress));

        SyncCheckpoint checkpoint = checkpointWriter.loadOrStart("11", 12, LocalDateTime.now());

        assertThat(checkpoint.nextPageNo()).isEqualTo(4);
    }

    @Test
    void advanceMovesToNextPage() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);
        when(syncCheckpointRepository.findById(new SyncCheckpointId("11", 12)))
                .thenReturn(Optional.of(checkpoint));

        checkpointWriter.advance("11", 12, 5);

        assertThat(checkpoint.nextPageNo()).isEqualTo(6);
    }

    @Test
    void completeMarksDone() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);
        when(syncCheckpointRepository.findById(new SyncCheckpointId("11", 12)))
                .thenReturn(Optional.of(checkpoint));

        checkpointWriter.complete("11", 12);

        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.DONE);
    }

    @Test
    void suspendByQuotaMarksQuotaExceeded() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);
        when(syncCheckpointRepository.findById(new SyncCheckpointId("11", 12)))
                .thenReturn(Optional.of(checkpoint));

        checkpointWriter.suspendByQuota("11", 12);

        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.QUOTA_EXCEEDED);
    }

    @Test
    void failCreatesCheckpointIfMissingThenMarksFailed() {
        when(syncCheckpointRepository.findById(new SyncCheckpointId("11", 12)))
                .thenReturn(Optional.empty());
        ArgumentCaptor<SyncCheckpoint> captor = ArgumentCaptor.forClass(SyncCheckpoint.class);

        checkpointWriter.fail("11", 12);

        verify(syncCheckpointRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SyncStatus.FAILED);
    }
}
