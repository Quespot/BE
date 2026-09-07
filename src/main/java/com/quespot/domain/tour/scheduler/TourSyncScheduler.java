package com.quespot.domain.tour.scheduler;

import com.quespot.domain.tour.service.TourContentCollector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 로컬에서 자동 실행되면 쿼터가 낭비된다. 기본 off.
@Component
@ConditionalOnProperty(name = "app.tour-api.scheduler.enabled", havingValue = "true")
public class TourSyncScheduler {

    private final TourContentCollector tourContentCollector;

    public TourSyncScheduler(TourContentCollector tourContentCollector) {
        this.tourContentCollector = tourContentCollector;
    }

    @Scheduled(cron = "${app.tour-api.scheduler.cron}")
    public void run() {
        tourContentCollector.collect();
    }
}
