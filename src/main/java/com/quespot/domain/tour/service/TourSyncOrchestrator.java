package com.quespot.domain.tour.service;

import com.quespot.domain.mission.dto.res.MissionCandidateGenerationResponseDTO;
import com.quespot.domain.mission.service.MissionCandidateGenerator;
import com.quespot.domain.spot.service.RefinementSummary;
import com.quespot.domain.spot.service.SpotRefiner;
import com.quespot.domain.tour.dto.res.TourSyncResponseDTO;
import com.quespot.domain.tour.exception.TourSyncException;
import com.quespot.domain.tour.exception.code.TourSyncErrorCode;
import com.quespot.global.monitoring.ServiceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourSyncOrchestrator {

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final TourContentCollector tourContentCollector;
    private final SpotRefiner spotRefiner;
    private final MissionCandidateGenerator missionCandidateGenerator;
    private final ServiceMetrics serviceMetrics;

    // TourAPI 동기화 로직
    public TourSyncResponseDTO synchronize() {
        if (!running.compareAndSet(false, true)) {
            throw new TourSyncException(TourSyncErrorCode.SYNC_ALREADY_RUNNING);
        }

        long startedAt = System.nanoTime();
        boolean success = false;
        try {
            log.info("TourAPI 전체 동기화 시작");
            tourContentCollector.collect();
            RefinementSummary refinement = spotRefiner.refine();
            MissionCandidateGenerationResponseDTO candidateGeneration = missionCandidateGenerator.generate();

            log.info(
                    "TourAPI 전체 동기화 완료: spotCreated={}, spotUpdated={}, candidateCreated={}",
                    refinement.created(),
                    refinement.updated(),
                    candidateGeneration.createdCount()
            );
            serviceMetrics.tourSyncItems("spot_created", refinement.created());
            serviceMetrics.tourSyncItems("spot_updated", refinement.updated());
            serviceMetrics.tourSyncItems("spot_failed", refinement.failed());
            serviceMetrics.tourSyncItems("candidate_created", candidateGeneration.createdCount());
            success = true;
            return new TourSyncResponseDTO(refinement, candidateGeneration);
        } finally {
            serviceMetrics.tourSyncFinished(success, Duration.ofNanos(System.nanoTime() - startedAt));
            running.set(false);
        }
    }
}
