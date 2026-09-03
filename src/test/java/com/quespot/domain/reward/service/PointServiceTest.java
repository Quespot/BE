package com.quespot.domain.reward.service;

import com.quespot.domain.reward.dto.res.PointResponseDTO;
import com.quespot.domain.reward.entity.UserPoint;
import com.quespot.domain.reward.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PointServiceTest {

    private UserPointRepository userPointRepository;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointRepository = mock(UserPointRepository.class);
        pointService = new PointService(userPointRepository);
    }

    @Test
    void returnsZeroPointsWhenUserHasNoLedgerRow() {
        Long userId = 1L;
        when(userPointRepository.findById(userId)).thenReturn(Optional.empty());

        PointResponseDTO result = pointService.getPoints(userId);

        assertThat(result).isEqualTo(new PointResponseDTO(0, 0, 0));
    }

    @Test
    void returnsStoredBalanceWhenLedgerRowExists() {
        Long userId = 1L;
        UserPoint userPoint = mock(UserPoint.class);
        when(userPoint.getBalance()).thenReturn(1240);
        when(userPoint.getTotalEarned()).thenReturn(1440);
        when(userPoint.getTotalSpent()).thenReturn(200);

        when(userPointRepository.findById(userId)).thenReturn(Optional.of(userPoint));

        PointResponseDTO result = pointService.getPoints(userId);

        assertThat(result).isEqualTo(new PointResponseDTO(1240, 1440, 200));
    }
}
