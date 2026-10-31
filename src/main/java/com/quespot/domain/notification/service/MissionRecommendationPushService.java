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
// 이름에 Push를 붙인 이유: domain/mission에 같은 이름의 MissionRecommendationService(#56, 추천 미션
// 조회 API)가 있어 기본 빈 이름이 충돌했다(ConflictingBeanDefinitionException). 패키지가 달라도
// 클래스명이 같으면 스프링 빈 이름이 겹치니 다른 도메인과 같은 클래스명을 쓰지 말 것.
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionRecommendationPushService {

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
