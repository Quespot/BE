package com.quespot.domain.notification.service;

import com.quespot.domain.notification.config.NotificationRecommendationProperties;
import com.quespot.domain.notification.dto.NotificationCommand;
import com.quespot.domain.notification.dto.RecommendableMissionDTO;
import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import com.quespot.domain.notification.enums.RecommendationOutcome;
import com.quespot.domain.notification.repository.NotificationRepository;
import com.quespot.domain.notification.repository.RecommendableMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

// 사용자 1명의 추천 판정 + 알림 생성. 별도 Bean의 @Transactional이라 사용자마다 독립 트랜잭션이고,
// 커밋 뒤 NotificationPushListener가 그 사용자 푸시를 보낸다. 한 사용자의 실패가 다른 사용자에게
// 번지지 않는다(MissionRecommendationPushService가 사용자별로 try/catch).
@Component
@RequiredArgsConstructor
public class MissionRecommendationNotifier {

    static final String TITLE = "근처에 미션이 있어요!";

    private final NotificationRepository notificationRepository;
    private final RecommendableMissionRepository recommendableMissionRepository;
    private final NotificationService notificationService;
    private final NotificationRecommendationProperties properties;

    @Transactional
    public RecommendationOutcome recommend(Long userId, BigDecimal latitude, BigDecimal longitude) {
        // "오늘"은 KST 기준이지만 created_at은 JVM 기본 시간대(운영 컨테이너는 UTC)의 LocalDateTime으로
        // 저장되므로, KST 자정을 JVM 시간대의 벽시계 값으로 바꿔 비교한다(#57 리뷰).
        ZoneId zone = ZoneId.of(properties.zone());
        LocalDateTime todayStart = LocalDate.now(zone).atStartOfDay(zone)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
        // 재시작 등으로 같은 날 두 번 돌아도 한 번만 보낸다.
        if (notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, NotificationType.MISSION_RECOMMENDATION, todayStart)) {
            return RecommendationOutcome.ALREADY_SENT_TODAY;
        }
        return recommendableMissionRepository
                .pickRandomNearby(userId, latitude, longitude, properties.radiusMeters())
                .map(mission -> {
                    notificationService.notify(toCommand(userId, mission));
                    return RecommendationOutcome.SENT;
                })
                .orElse(RecommendationOutcome.NO_MISSION_NEARBY);
    }

    private NotificationCommand toCommand(Long userId, RecommendableMissionDTO mission) {
        String body = String.format("%s에서 '%s' 미션에 도전해 보세요 (+%d포인트)",
                mission.spotName(), mission.title(), mission.rewardPoint());
        return new NotificationCommand(
                userId, NotificationType.MISSION_RECOMMENDATION, TITLE, body,
                NotificationReferenceType.MISSION, mission.missionId()
        );
    }
}
