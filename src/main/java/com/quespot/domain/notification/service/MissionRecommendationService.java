package com.quespot.domain.notification.service;

import com.quespot.domain.notification.config.NotificationRecommendationProperties;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.RecommendationOutcome;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

// 스케줄러 진입점. 트랜잭션을 걸지 않는다 — 사용자별 트랜잭션은 Notifier가 연다.
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionRecommendationService {

    private final FcmTokenRepository fcmTokenRepository;
    private final MissionRecommendationNotifier notifier;
    private final NotificationRecommendationProperties properties;

    public void recommendToAll() {
        LocalDateTime since = LocalDateTime.now().minusDays(properties.locationMaxAgeDays());
        Map<Long, FcmToken> freshestByUser = new LinkedHashMap<>();
        for (FcmToken token : fcmTokenRepository.findLocatedTokensOfPushEnabledUsers(since)) {
            freshestByUser.merge(token.getUserId(), token,
                    (current, candidate) -> candidate.getLocatedAt().isAfter(current.getLocatedAt()) ? candidate : current);
        }

        Map<RecommendationOutcome, Integer> summary = new EnumMap<>(RecommendationOutcome.class);
        int failed = 0;
        for (FcmToken token : freshestByUser.values()) {
            try {
                RecommendationOutcome outcome =
                        notifier.recommend(token.getUserId(), token.getLastLatitude(), token.getLastLongitude());
                summary.merge(outcome, 1, Integer::sum);
            } catch (RuntimeException exception) {
                failed++;
                log.warn("미션 추천 알림 실패. userId={}", token.getUserId(), exception);
            }
        }
        log.info("미션 추천 알림 완료. users={}, sent={}, alreadySentToday={}, noMissionNearby={}, failed={}",
                freshestByUser.size(),
                summary.getOrDefault(RecommendationOutcome.SENT, 0),
                summary.getOrDefault(RecommendationOutcome.ALREADY_SENT_TODAY, 0),
                summary.getOrDefault(RecommendationOutcome.NO_MISSION_NEARBY, 0),
                failed);
    }
}
