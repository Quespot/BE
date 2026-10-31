package com.quespot.domain.reward.service;

import com.quespot.domain.reward.dto.res.AchievementSummaryResponseDTO;
import com.quespot.domain.reward.entity.UserPoint;
import com.quespot.domain.reward.repository.AchievementMetricRepository;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.StampRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import com.quespot.domain.reward.repository.UserPointRepository;
import com.quespot.domain.reward.repository.UserStampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AchievementQueryServiceTest {

    private AchievementMetricRepository metricRepository;
    private UserPointRepository userPointRepository;
    private UserBadgeRepository userBadgeRepository;
    private BadgeRepository badgeRepository;
    private UserStampRepository userStampRepository;
    private StampRepository stampRepository;
    private AchievementQueryService service;

    @BeforeEach
    void setUp() {
        metricRepository = mock(AchievementMetricRepository.class);
        userPointRepository = mock(UserPointRepository.class);
        userBadgeRepository = mock(UserBadgeRepository.class);
        badgeRepository = mock(BadgeRepository.class);
        userStampRepository = mock(UserStampRepository.class);
        stampRepository = mock(StampRepository.class);
        service = new AchievementQueryService(
                metricRepository, userPointRepository, userBadgeRepository, badgeRepository, userStampRepository, stampRepository
        );
    }

    @Test
    void summarizesCountsWithServerSideDenominators() {
        UserPoint point = mock(UserPoint.class);
        when(point.getBalance()).thenReturn(350);
        when(metricRepository.countCompletedMissions(7L, null)).thenReturn(2L);
        when(userPointRepository.findById(7L)).thenReturn(Optional.of(point));
        when(userBadgeRepository.countByUserIdAndBadge_IsActiveTrue(7L)).thenReturn(2L);
        when(badgeRepository.countByIsActiveTrue()).thenReturn(5L);
        when(userStampRepository.countByUserId(7L)).thenReturn(1L);
        when(stampRepository.count()).thenReturn(8L);

        AchievementSummaryResponseDTO result = service.getSummary(7L);

        assertThat(result).isEqualTo(new AchievementSummaryResponseDTO(2L, 350, 2L, 5L, 1L, 8L));
    }

    @Test
    void returnsZeroPointWhenUserHasNoPointRow() {
        when(userPointRepository.findById(7L)).thenReturn(Optional.empty());

        AchievementSummaryResponseDTO result = service.getSummary(7L);

        assertThat(result.totalPoint()).isZero();
    }
}
