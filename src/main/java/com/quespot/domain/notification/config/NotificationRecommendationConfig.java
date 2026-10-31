package com.quespot.domain.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationRecommendationProperties.class)
public class NotificationRecommendationConfig {
}
