package com.quespot.domain.reward.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.reward.dto.BadgeCondition;
import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.entity.UserBadge;
import com.quespot.domain.reward.entity.UserStamp;
import com.quespot.domain.reward.enums.AchievementMetric;
import com.quespot.domain.reward.enums.ActivityType;
import com.quespot.domain.reward.repository.AchievementMetricRepository;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.RewardActivityRepository;
import com.quespot.domain.reward.repository.StampRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import com.quespot.domain.reward.repository.UserStampRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// 배지·스탬프 획득 판정. 호출부(MissionArrivalService / MissionPhotoService /
// ArchivePhotoService)의 트랜잭션에 합류한다(REQUIRED) — "미션은 완료됐는데
// 배지 누락"이 없어야 한다. 중복 획득은 UNIQUE(user_id, badge_id)/(user_id,
// stamp_id)가 막고 애플리케이션 락은 쓰지 않는다. 배지 획득에 포인트는 없다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private static final Set<AchievementMetric> MISSION_COMPLETION_METRICS = EnumSet.of(
            AchievementMetric.MISSION_COMPLETED,
            AchievementMetric.MISSION_COMPLETED_DISTINCT_DISTRICT,
            AchievementMetric.COURSE_COMPLETED
    );

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final StampRepository stampRepository;
    private final UserStampRepository userStampRepository;
    private final RewardActivityRepository rewardActivityRepository;
    private final AchievementMetricRepository metricRepository;
    private final ObjectMapper objectMapper;

    // 코스 완주 판정(CourseAttemptService) 뒤에 호출돼야 COURSE_COMPLETED가
    // 같은 호출에서 즉시 반영된다 — MissionArrivalService.arrive()의 순서 참고.
    @Transactional
    public void onMissionCompleted(Long userId, String regionCode) {
        evaluateStamp(userId, regionCode);
        evaluateBadges(userId, MISSION_COMPLETION_METRICS);
    }

    @Transactional
    public void onPhotoRegistered(Long userId) {
        evaluateBadges(userId, EnumSet.of(AchievementMetric.PHOTO_REGISTERED));
    }

    // is_active와 무관하게 8개 시도 전부 판정한다 — 활성은 "수집 가능" 노출용 플래그.
    private void evaluateStamp(Long userId, String regionCode) {
        if (regionCode == null) {
            return;
        }
        stampRepository.findByRegionCode(regionCode)
                .filter(stamp -> !userStampRepository.existsByUserIdAndStamp_Id(userId, stamp.getId()))
                .ifPresent(stamp -> {
                    userStampRepository.save(UserStamp.acquire(userId, stamp));
                    rewardActivityRepository.save(RewardActivity.of(
                            userId, ActivityType.STAMP_ACQUIRED, stamp.getName() + " 스탬프 획득",
                            null, "STAMP", stamp.getId()
                    ));
                });
    }

    // 배지가 5개뿐이라 활성 전체를 한 번 읽어 메모리에서 거른다. 수십 개로 늘면
    // metric 생성 컬럼 + 인덱스로 전환한다(스펙 2.3).
    private void evaluateBadges(Long userId, Set<AchievementMetric> affectedMetrics) {
        List<Badge> candidates = badgeRepository.findByIsActiveTrue();
        if (candidates.isEmpty()) {
            return;
        }
        Set<Long> acquiredIds = new HashSet<>(userBadgeRepository.findBadgeIdsByUserId(userId));
        Map<String, Long> memo = new HashMap<>();
        for (Badge badge : candidates) {
            if (acquiredIds.contains(badge.getId())) {
                continue;
            }
            Optional<BadgeCondition> parsed = BadgeCondition.parse(objectMapper, badge.getConditionJson());
            if (parsed.isEmpty()) {
                log.warn("배지 조건 파싱 실패 — 건너뜀. code={}, json={}", badge.getCode(), badge.getConditionJson());
                continue;
            }
            BadgeCondition condition = parsed.get();
            if (!affectedMetrics.contains(condition.metric())) {
                continue;
            }
            long value = memo.computeIfAbsent(
                    condition.metric() + ":" + condition.regionCode(),
                    key -> measure(userId, condition)
            );
            if (value >= condition.threshold()) {
                userBadgeRepository.save(UserBadge.acquire(userId, badge));
                rewardActivityRepository.save(RewardActivity.of(
                        userId, ActivityType.BADGE_ACQUIRED, badge.getName() + " 배지 획득",
                        null, "BADGE", badge.getId()
                ));
            }
        }
    }

    private long measure(Long userId, BadgeCondition condition) {
        return switch (condition.metric()) {
            case MISSION_COMPLETED -> metricRepository.countCompletedMissions(userId, condition.regionCode());
            case MISSION_COMPLETED_DISTINCT_DISTRICT -> condition.regionCode() == null
                    ? 0L
                    : metricRepository.countDistinctDistricts(userId, condition.regionCode());
            case PHOTO_REGISTERED -> metricRepository.countMissionPhotos(userId) + metricRepository.countArchivePhotos(userId);
            case COURSE_COMPLETED -> metricRepository.countCompletedCourses(userId);
        };
    }
}
