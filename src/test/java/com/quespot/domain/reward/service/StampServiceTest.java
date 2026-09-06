package com.quespot.domain.reward.service;

import com.quespot.domain.reward.dto.res.StampListResponseDTO;
import com.quespot.domain.reward.entity.Stamp;
import com.quespot.domain.reward.entity.UserStamp;
import com.quespot.domain.reward.repository.StampRepository;
import com.quespot.domain.reward.repository.UserStampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StampServiceTest {

    private StampRepository stampRepository;
    private UserStampRepository userStampRepository;
    private StampService stampService;

    @BeforeEach
    void setUp() {
        stampRepository = mock(StampRepository.class);
        userStampRepository = mock(UserStampRepository.class);
        stampService = new StampService(stampRepository, userStampRepository);
    }

    private Stamp stamp(Long id, String code, String name, boolean isActive) {
        Stamp stamp = mock(Stamp.class);
        when(stamp.getId()).thenReturn(id);
        when(stamp.getCode()).thenReturn(code);
        when(stamp.getName()).thenReturn(name);
        when(stamp.getIsActive()).thenReturn(isActive);
        return stamp;
    }

    private UserStamp acquired(Stamp stamp, LocalDateTime acquiredAt) {
        UserStamp userStamp = mock(UserStamp.class);
        when(userStamp.getStamp()).thenReturn(stamp);
        when(userStamp.getAcquiredAt()).thenReturn(acquiredAt);
        return userStamp;
    }

    @Test
    void returnsLockedInactiveStampsAlongsideActiveOne() {
        Long userId = 1L;
        Stamp seoul = stamp(1L, "SEOUL", "서울", true);
        Stamp busan = stamp(2L, "BUSAN", "부산", false);

        when(stampRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(seoul, busan));
        when(userStampRepository.findByUserId(userId)).thenReturn(List.of());

        StampListResponseDTO result = stampService.getStamps(userId);

        assertThat(result.stamps()).hasSize(2);
        assertThat(result.stamps().get(0).isActive()).isTrue();
        assertThat(result.stamps().get(1).isActive()).isFalse();
        assertThat(result.stamps()).allMatch(dto -> !dto.acquired());
    }

    @Test
    void marksOnlyMatchingStampAsAcquired() {
        Long userId = 1L;
        Stamp seoul = stamp(1L, "SEOUL", "서울", true);
        LocalDateTime acquiredAt = LocalDateTime.of(2026, 9, 6, 10, 0);

        UserStamp acquiredSeoul = acquired(seoul, acquiredAt);

        when(stampRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(seoul));
        when(userStampRepository.findByUserId(userId)).thenReturn(List.of(acquiredSeoul));

        StampListResponseDTO result = stampService.getStamps(userId);

        assertThat(result.stamps().get(0).acquired()).isTrue();
        assertThat(result.stamps().get(0).acquiredAt()).isEqualTo(acquiredAt);
    }
}
