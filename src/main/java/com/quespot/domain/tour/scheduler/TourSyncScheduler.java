package com.quespot.domain.tour.scheduler;

import com.quespot.domain.tour.service.TourSyncOrchestrator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tour-api.scheduler.enabled", havingValue = "true")
public class TourSyncScheduler {

    private final TourSyncOrchestrator tourSyncOrchestrator;

    public TourSyncScheduler(TourSyncOrchestrator tourSyncOrchestrator) {
        this.tourSyncOrchestrator = tourSyncOrchestrator;
    }

    // TourAPI 정기 동기화 로직
    @Scheduled(
            cron = "${app.tour-api.scheduler.cron}",
            zone = "${app.tour-api.scheduler.zone:Asia/Seoul}"
    )
    public void run() {
        tourSyncOrchestrator.synchronize();
    }
}
