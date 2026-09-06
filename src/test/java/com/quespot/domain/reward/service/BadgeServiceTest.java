package com.quespot.domain.reward.service;

import com.quespot.domain.reward.dto.res.BadgeListResponseDTO;
import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.UserBadge;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BadgeServiceTest {

    private BadgeRepository badgeRepository;
    private UserBadgeRepository userBadgeRepository;
    private BadgeService badgeService;

    @BeforeEach
    void setUp() {
        badgeRepository = mock(BadgeRepository.class);
        userBadgeRepository = mock(UserBadgeRepository.class);
        badgeService = new BadgeService(badgeRepository, userBadgeRepository);
    }

    private Badge badge(Long id, String code, String name) {
        Badge badge = mock(Badge.class);
        when(badge.getId()).thenReturn(id);
        when(badge.getCode()).thenReturn(code);
        when(badge.getName()).thenReturn(name);
        return badge;
    }

    private UserBadge acquired(Badge badge, LocalDateTime acquiredAt) {
        UserBadge userBadge = mock(UserBadge.class);
        when(userBadge.getBadge()).thenReturn(badge);
        when(userBadge.getAcquiredAt()).thenReturn(acquiredAt);
        return userBadge;
    }

    @Test
    void marksAllBadgesUnacquiredWhenUserHasNoRecords() {
        Long userId = 1L;
        Badge firstMission = badge(1L, "FIRST_MISSION", "첫 미션");
        Badge explorer = badge(2L, "EXPLORER", "탐험가");

        when(badgeRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(firstMission, explorer));
        when(userBadgeRepository.findByUserId(userId)).thenReturn(List.of());

        BadgeListResponseDTO result = badgeService.getBadges(userId);

        assertThat(result.badges()).hasSize(2);
        assertThat(result.badges()).allMatch(dto -> !dto.acquired() && dto.acquiredAt() == null);
    }

    @Test
    void marksOnlyMatchingBadgeAsAcquired() {
        Long userId = 1L;
        Badge firstMission = badge(1L, "FIRST_MISSION", "첫 미션");
        Badge explorer = badge(2L, "EXPLORER", "탐험가");
        LocalDateTime acquiredAt = LocalDateTime.of(2026, 9, 6, 10, 0);

        UserBadge acquiredFirstMission = acquired(firstMission, acquiredAt);

        when(badgeRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(firstMission, explorer));
        when(userBadgeRepository.findByUserId(userId)).thenReturn(List.of(acquiredFirstMission));

        BadgeListResponseDTO result = badgeService.getBadges(userId);

        assertThat(result.badges()).hasSize(2);
        assertThat(result.badges().get(0).acquired()).isTrue();
        assertThat(result.badges().get(0).acquiredAt()).isEqualTo(acquiredAt);
        assertThat(result.badges().get(1).acquired()).isFalse();
        assertThat(result.badges().get(1).acquiredAt()).isNull();
    }
}
