package com.quespot.domain.reward.service;

import com.quespot.domain.reward.converter.RewardConverter;
import com.quespot.domain.reward.dto.res.BadgeListResponseDTO;
import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.UserBadge;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Transactional(readOnly = true)
    public BadgeListResponseDTO getBadges(Long userId) {
        List<Badge> badges = badgeRepository.findAllByOrderBySortOrderAsc();

        Map<Long, UserBadge> acquiredByBadgeId = userBadgeRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), Function.identity()));

        return new BadgeListResponseDTO(
                badges.stream()
                        .map(badge -> RewardConverter.toBadgeResponseDTO(badge, acquiredByBadgeId.get(badge.getId())))
                        .toList()
        );
    }
}
