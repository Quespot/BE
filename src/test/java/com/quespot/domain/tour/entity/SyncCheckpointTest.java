package com.quespot.domain.tour.entity;

import com.quespot.domain.tour.enums.SyncStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SyncCheckpointTest {

    @Test
    void initStartsAtPageOneWithRunningStatusAndNoModifiedTime() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);

        assertThat(checkpoint.getLdongRegnCd()).isEqualTo("11");
        assertThat(checkpoint.getContentTypeId()).isEqualTo(12);
        assertThat(checkpoint.nextPageNo()).isEqualTo(1);
        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.RUNNING);
        assertThat(checkpoint.getLastModifiedTime()).isNull();
        assertThat(checkpoint.isInitialLoadDone()).isFalse();
    }

    @Test
    void advanceSetsNextPageToGivenPagePlusOne() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);

        checkpoint.advance(3);

        assertThat(checkpoint.nextPageNo()).isEqualTo(4);
        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.RUNNING);
    }

    @Test
    void completeResetsPageToZeroAndMarksDone() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);
        checkpoint.advance(5);

        checkpoint.complete();

        assertThat(checkpoint.getLastPageNo()).isEqualTo(0);
        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.DONE);
        assertThat(checkpoint.isInitialLoadDone()).isTrue();
    }

    @Test
    void suspendByQuotaKeepsPageButMarksQuotaExceeded() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);
        checkpoint.advance(2);

        checkpoint.suspendByQuota();

        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.QUOTA_EXCEEDED);
        assertThat(checkpoint.nextPageNo()).isEqualTo(3);
    }

    @Test
    void failMarksFailedStatus() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);

        checkpoint.fail();

        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.FAILED);
    }

    @Test
    void startIncrementalRunResetsPageToOneAndRecordsModifiedTime() {
        SyncCheckpoint checkpoint = SyncCheckpoint.init("11", 12);
        checkpoint.complete();
        LocalDateTime yesterday = LocalDateTime.of(2026, 9, 6, 0, 0);

        checkpoint.startIncrementalRun(yesterday);

        assertThat(checkpoint.nextPageNo()).isEqualTo(1);
        assertThat(checkpoint.getLastModifiedTime()).isEqualTo(yesterday);
        assertThat(checkpoint.getStatus()).isEqualTo(SyncStatus.RUNNING);
        assertThat(checkpoint.isInitialLoadDone()).isFalse();
    }
}
