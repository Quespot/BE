package com.quespot.domain.tour.service;

import com.quespot.domain.tour.repository.ApiQuotaUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuotaGuardTest {

    private ApiQuotaUsageRepository apiQuotaUsageRepository;
    private QuotaGuard quotaGuard;

    @BeforeEach
    void setUp() {
        apiQuotaUsageRepository = mock(ApiQuotaUsageRepository.class);
        quotaGuard = new QuotaGuard(apiQuotaUsageRepository, 1000);
    }

    @Test
    void returnsTrueWhenUpdateAffectsARow() {
        when(apiQuotaUsageRepository.tryIncrement(any(), eq(1000))).thenReturn(1);

        boolean consumed = quotaGuard.tryConsume();

        assertThat(consumed).isTrue();
        verify(apiQuotaUsageRepository).insertIfAbsent(any(LocalDate.class));
        verify(apiQuotaUsageRepository).tryIncrement(any(LocalDate.class), eq(1000));
    }

    @Test
    void returnsFalseWhenDailyLimitAlreadyReached() {
        when(apiQuotaUsageRepository.tryIncrement(any(), eq(1000))).thenReturn(0);

        boolean consumed = quotaGuard.tryConsume();

        assertThat(consumed).isFalse();
    }
}
