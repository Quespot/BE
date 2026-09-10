package com.quespot.domain.reward.service;

import com.quespot.domain.reward.dto.res.AchievementSummaryResponseDTO;
import com.quespot.domain.reward.entity.UserPoint;
import com.quespot.domain.reward.repository.AchievementMetricRepository;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.StampRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import com.quespot.domain.reward.repository.UserPointRepository;
import com.quespot.domain.reward.repository.UserStampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// COUNT 쿼리만 쓴다. 목록을 가져와 세지 않는다.
@Service
@RequiredArgsConstructor
public class AchievementQueryService {

    private final AchievementMetricRepository metricRepository;
    private final UserPointRepository userPointRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final UserStampRepository userStampRepository;
    private final StampRepository stampRepository;

    @Transactional(readOnly = true)
    public AchievementSummaryResponseDTO getSummary(Long userId) {
        return new AchievementSummaryResponseDTO(
                metricRepository.countCompletedMissions(userId, null),
                userPointRepository.findById(userId).map(UserPoint::getBalance).orElse(0),
                userBadgeRepository.countByUserIdAndBadge_IsActiveTrue(userId),
                badgeRepository.countByIsActiveTrue(),
                userStampRepository.countByUserId(userId),
                stampRepository.count()
        );
    }
}
