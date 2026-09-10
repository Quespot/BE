package com.quespot.domain.like.service;

import com.quespot.domain.like.exception.LikeException;
import com.quespot.domain.like.exception.code.LikeErrorCode;
import com.quespot.domain.like.repository.SavedSpotRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SavedSpotServiceTest {

    private SavedSpotRepository savedSpotRepository;
    private SpotRepository spotRepository;
    private SavedSpotService savedSpotService;

    @BeforeEach
    void setUp() {
        savedSpotRepository = mock(SavedSpotRepository.class);
        spotRepository = mock(SpotRepository.class);
        savedSpotService = new SavedSpotService(savedSpotRepository, spotRepository);
    }

    @Test
    void saveUpsertsWhenSpotVisible() {
        Spot visible = mock(Spot.class);
        when(visible.getShowFlag()).thenReturn(true);
        when(spotRepository.findById(3L)).thenReturn(Optional.of(visible));

        savedSpotService.save(7L, 3L);

        verify(savedSpotRepository).upsert(7L, 3L);
    }

    @Test
    void saveThrowsWhenSpotMissing() {
        when(spotRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedSpotService.save(7L, 3L))
                .isInstanceOf(LikeException.class)
                .extracting(e -> ((LikeException) e).getErrorCode())
                .isEqualTo(LikeErrorCode.SPOT_NOT_FOUND);
        verifyNoInteractions(savedSpotRepository);
    }

    @Test
    void saveThrowsWhenSpotHidden() {
        Spot hidden = mock(Spot.class);
        when(hidden.getShowFlag()).thenReturn(false);
        when(spotRepository.findById(3L)).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> savedSpotService.save(7L, 3L))
                .isInstanceOf(LikeException.class);
        verifyNoInteractions(savedSpotRepository);
    }

    @Test
    void unsaveDeletesWithoutValidation() {
        savedSpotService.unsave(7L, 3L);

        verify(savedSpotRepository).deleteByUserIdAndSpot_Id(7L, 3L);
        verifyNoInteractions(spotRepository);
    }
}
