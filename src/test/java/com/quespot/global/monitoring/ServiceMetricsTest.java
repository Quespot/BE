package com.quespot.global.monitoring;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ServiceMetrics serviceMetrics = new ServiceMetrics(meterRegistry);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void incrementsImmediatelyWithoutTransactionSynchronization() {
        serviceMetrics.userSignedUp("EMAIL");
        serviceMetrics.missionCompleted("11");
        serviceMetrics.tourApiCall(true);

        assertThat(signups()).isEqualTo(1);
        assertThat(missionCompleted()).isEqualTo(1);
        assertThat(tourApiCalls()).isEqualTo(1);
    }

    @Test
    void incrementsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        serviceMetrics.userSignedUp("EMAIL");
        serviceMetrics.missionCompleted("11");
        serviceMetrics.tourApiCall(true);

        assertThat(signups()).isZero();
        assertThat(missionCompleted()).isZero();
        assertThat(tourApiCalls()).isZero();

        complete(TransactionSynchronization::afterCommit);

        assertThat(signups()).isEqualTo(1);
        assertThat(missionCompleted()).isEqualTo(1);
        assertThat(tourApiCalls()).isEqualTo(1);
    }

    @Test
    void doesNotIncrementAfterRollback() {
        TransactionSynchronizationManager.initSynchronization();

        serviceMetrics.userSignedUp("EMAIL");
        serviceMetrics.missionCompleted("11");
        serviceMetrics.tourApiCall(true);

        complete(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(signups()).isZero();
        assertThat(missionCompleted()).isZero();
        assertThat(tourApiCalls()).isZero();
    }

    private void complete(Consumer<TransactionSynchronization> action) {
        TransactionSynchronizationManager.getSynchronizations().forEach(action);
        TransactionSynchronizationManager.clearSynchronization();
    }

    private double signups() {
        return count("quespot.user.signups");
    }

    private double missionCompleted() {
        return count("quespot.mission.completed");
    }

    private double tourApiCalls() {
        return count("quespot.tourapi.calls");
    }

    private double count(String name) {
        return meterRegistry.find(name).counters().stream().mapToDouble(c -> c.count()).sum();
    }
}
