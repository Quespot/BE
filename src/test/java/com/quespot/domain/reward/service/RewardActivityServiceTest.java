package com.quespot.domain.reward.service;

import com.quespot.domain.reward.dto.res.RewardActivityListResponseDTO;
import com.quespot.domain.reward.entity.PointTransaction;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.enums.ActivityType;
import com.quespot.domain.reward.repository.RewardActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardActivityServiceTest {

    private RewardActivityRepository rewardActivityRepository;
    private RewardActivityService rewardActivityService;

    @BeforeEach
    void setUp() {
        rewardActivityRepository = mock(RewardActivityRepository.class);
        rewardActivityService = new RewardActivityService(rewardActivityRepository);
    }

    private RewardActivity activity(Long id, ActivityType type, String title, PointTransaction pointTransaction) {
        RewardActivity activity = mock(RewardActivity.class);
        when(activity.getId()).thenReturn(id);
        when(activity.getActivityType()).thenReturn(type);
        when(activity.getTitle()).thenReturn(title);
        when(activity.getPointTransaction()).thenReturn(pointTransaction);
        when(activity.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 8, 12, 0));
        return activity;
    }

    private PointTransaction transactionWithAmount(int amount) {
        PointTransaction pointTransaction = mock(PointTransaction.class);
        when(pointTransaction.getAmount()).thenReturn(amount);
        return pointTransaction;
    }

    @Test
    void mapsBadgeActivityAmountToNullWhenNoPointTransactionLinked() {
        Long userId = 1L;
        RewardActivity badgeActivity = activity(5L, ActivityType.BADGE_ACQUIRED, "배지 '탐험가' 획득", null);

        when(rewardActivityRepository.findFeed(eq(userId), isNull(), any()))
                .thenReturn(List.of(badgeActivity));

        RewardActivityListResponseDTO result = rewardActivityService.getActivities(userId, null, 20);

        assertThat(result.activities()).hasSize(1);
        assertThat(result.activities().get(0).amount()).isNull();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void mapsPointEarnedActivityAmountFromLinkedTransaction() {
        Long userId = 1L;
        RewardActivity earnedActivity =
                activity(4L, ActivityType.POINT_EARNED, "북촌 한옥마을 미션 완료", transactionWithAmount(200));

        when(rewardActivityRepository.findFeed(eq(userId), isNull(), any()))
                .thenReturn(List.of(earnedActivity));

        RewardActivityListResponseDTO result = rewardActivityService.getActivities(userId, null, 20);

        assertThat(result.activities().get(0).amount()).isEqualTo(200);
    }

    @Test
    void setsHasNextAndTrimsExtraRowWhenMoreItemsExistThanSize() {
        Long userId = 1L;
        int size = 2;
        RewardActivity first = activity(3L, ActivityType.POINT_EARNED, "A", transactionWithAmount(10));
        RewardActivity second = activity(2L, ActivityType.POINT_EARNED, "B", transactionWithAmount(20));
        RewardActivity extra = activity(1L, ActivityType.POINT_EARNED, "C", transactionWithAmount(30));

        when(rewardActivityRepository.findFeed(eq(userId), isNull(), any()))
                .thenReturn(List.of(first, second, extra));

        RewardActivityListResponseDTO result = rewardActivityService.getActivities(userId, null, size);

        assertThat(result.activities()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2L);
    }

    @Test
    void passesCursorThroughToRepositoryForNextPageRequest() {
        Long userId = 1L;
        Long cursor = 10L;

        when(rewardActivityRepository.findFeed(eq(userId), eq(cursor), any()))
                .thenReturn(List.of());

        RewardActivityListResponseDTO result = rewardActivityService.getActivities(userId, cursor, 20);

        assertThat(result.activities()).isEmpty();
        verify(rewardActivityRepository).findFeed(eq(userId), eq(cursor), any());
    }
}
