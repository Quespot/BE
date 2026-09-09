package com.quespot.domain.reward.service;

import com.quespot.domain.reward.entity.PointTransaction;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.entity.UserPoint;
import com.quespot.domain.reward.repository.PointTransactionRepository;
import com.quespot.domain.reward.repository.RewardActivityRepository;
import com.quespot.domain.reward.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointServiceTest {

    private UserPointRepository userPointRepository;
    private PointTransactionRepository pointTransactionRepository;
    private RewardActivityRepository rewardActivityRepository;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointRepository = mock(UserPointRepository.class);
        pointTransactionRepository = mock(PointTransactionRepository.class);
        rewardActivityRepository = mock(RewardActivityRepository.class);
        pointService = new PointService(userPointRepository, pointTransactionRepository, rewardActivityRepository);

        when(pointTransactionRepository.save(any(PointTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UserPoint userPointWithBalance(int balance) {
        UserPoint userPoint = mock(UserPoint.class);
        when(userPoint.getBalance()).thenReturn(balance);
        return userPoint;
    }

    @Test
    void creditUpsertsBalanceThenInsertsTransactionAndActivity() {
        UserPoint userPoint = userPointWithBalance(300);
        when(userPointRepository.findById(1L)).thenReturn(Optional.of(userPoint));

        pointService.credit(1L, 100, "MISSION_REWARD", "MISSION_ATTEMPT", 55L, "미션 완료 보상");

        verify(userPointRepository).creditBalance(1L, 100);
        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(txCaptor.getValue().getAmount()).isEqualTo(100);
        assertThat(txCaptor.getValue().getType()).isEqualTo("MISSION_REWARD");
        assertThat(txCaptor.getValue().getReferenceType()).isEqualTo("MISSION_ATTEMPT");
        assertThat(txCaptor.getValue().getReferenceId()).isEqualTo(55L);
        assertThat(txCaptor.getValue().getBalanceAfter()).isEqualTo(300);

        ArgumentCaptor<RewardActivity> activityCaptor = ArgumentCaptor.forClass(RewardActivity.class);
        verify(rewardActivityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(activityCaptor.getValue().getTitle()).isEqualTo("미션 완료 보상");
        assertThat(activityCaptor.getValue().getReferenceType()).isEqualTo("MISSION_ATTEMPT");
        assertThat(activityCaptor.getValue().getReferenceId()).isEqualTo(55L);
    }
}
