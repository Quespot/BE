package com.quespot.domain.tour.service;

import com.quespot.domain.mission.dto.res.MissionCandidateGenerationResponseDTO;
import com.quespot.domain.mission.service.MissionCandidateGenerator;
import com.quespot.domain.spot.service.RefinementSummary;
import com.quespot.domain.spot.service.SpotRefiner;
import com.quespot.domain.tour.dto.res.TourSyncResponseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourSyncOrchestratorTest {

    @Test
    void collectsRefinesAndGeneratesCandidatesInOrder() {
        TourContentCollector collector = mock(TourContentCollector.class);
        SpotRefiner refiner = mock(SpotRefiner.class);
        MissionCandidateGenerator generator = mock(MissionCandidateGenerator.class);
        TourSyncOrchestrator orchestrator = new TourSyncOrchestrator(collector, refiner, generator);
        RefinementSummary refinement = new RefinementSummary(3, 2, 0, 1, 0);
        MissionCandidateGenerationResponseDTO generation =
                new MissionCandidateGenerationResponseDTO(5, 3, 2);
        when(refiner.refine()).thenReturn(refinement);
        when(generator.generate()).thenReturn(generation);

        TourSyncResponseDTO result = orchestrator.synchronize();

        InOrder inOrder = inOrder(collector, refiner, generator);
        inOrder.verify(collector).collect();
        inOrder.verify(refiner).refine();
        inOrder.verify(generator).generate();
        assertThat(result.refinement()).isEqualTo(refinement);
        assertThat(result.candidateGeneration()).isEqualTo(generation);
    }
}
