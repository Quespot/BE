package com.quespot.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.recommendation")
public record NotificationRecommendationProperties(
        boolean enabled,
        String cron,
        String zone,
        int radiusMeters,
        int locationMaxAgeDays
) {
}
