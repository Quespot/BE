package com.quespot.global.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ServiceMetrics {

    private static final String UNKNOWN = "UNKNOWN";

    private final MeterRegistry meterRegistry;

    public void missionCompleted(String regionCode) {
        String region = regionCode == null ? UNKNOWN : regionCode;
        afterCommit(() -> meterRegistry.counter("quespot.mission.completed", "region", region).increment());
    }

    public void missionArrivalRejected() {
        meterRegistry.counter("quespot.mission.arrival.rejected").increment();
    }

    public void userSignedUp(String provider) {
        afterCommit(() -> meterRegistry.counter("quespot.user.signups", "provider", provider).increment());
    }

    public void tourApiCall(boolean allowed) {
        String result = allowed ? "allowed" : "blocked";
        afterCommit(() -> meterRegistry.counter("quespot.tourapi.calls", "result", result).increment());
    }

    public void registerTourApiDailyLimit(int dailyLimit) {
        Gauge.builder("quespot.tourapi.daily.limit", () -> dailyLimit).register(meterRegistry);
    }

    public void tourSyncFinished(boolean success, Duration duration) {
        Timer.builder("quespot.tourapi.sync")
                .tag("result", success ? "success" : "failure")
                .register(meterRegistry)
                .record(duration);
    }

    public void tourSyncItems(String type, int count) {
        meterRegistry.counter("quespot.tourapi.sync.items", "type", type).increment(count);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
