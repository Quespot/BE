package com.quespot.domain.notification.scheduler;

import com.quespot.domain.notification.service.MissionRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 기본 꺼짐(app.notification.recommendation.enabled=false). 운영 EC2에서만 켠다.
// EC2 단일 인스턴스라 분산 락은 두지 않는다.
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.recommendation.enabled", havingValue = "true")
public class MissionRecommendationScheduler {

    private final MissionRecommendationService missionRecommendationService;

    @Scheduled(
            cron = "${app.notification.recommendation.cron}",
            zone = "${app.notification.recommendation.zone:Asia/Seoul}"
    )
    public void run() {
        missionRecommendationService.recommendToAll();
    }
}
