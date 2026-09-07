package com.quespot.domain.spot.service;

import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// @Transactional을 걸지 않는다 — 청크별 독립 커밋(SpotRefinementWriter)을 위해서다.
@Slf4j
@Component
public class SpotRefiner {

    private static final String OPERATION = "areaBasedSyncList2";
    private static final int CHUNK_SIZE = 20;

    private final TourContentRawRepository tourContentRawRepository;
    private final SpotRefinementWriter spotRefinementWriter;

    public SpotRefiner(
            TourContentRawRepository tourContentRawRepository,
            SpotRefinementWriter spotRefinementWriter
    ) {
        this.tourContentRawRepository = tourContentRawRepository;
        this.spotRefinementWriter = spotRefinementWriter;
    }

    public RefinementSummary refine() {
        List<TourContentRaw> latestPerContentId = latestPerContentId();

        List<RefinementSummary> chunkResults = new ArrayList<>();
        for (List<TourContentRaw> chunk : partition(latestPerContentId, CHUNK_SIZE)) {
            chunkResults.add(spotRefinementWriter.refineChunk(chunk));
        }

        RefinementSummary summary = RefinementSummary.combine(chunkResults);
        log.info(
                "정제 완료: 대상 {}건, 생성 {}건, 갱신 {}건, 좌표없음스킵 {}건, 변경없음스킵 {}건, 실패 {}건",
                latestPerContentId.size(), summary.created(), summary.updated(),
                summary.skippedInvalidCoordinates(), summary.skippedUnchanged(), summary.failed()
        );
        return summary;
    }

    // content_id당 api_modified_time이 가장 최신인 raw 행만 남긴다.
    private List<TourContentRaw> latestPerContentId() {
        List<TourContentRaw> sortedDesc =
                tourContentRawRepository.findByOperationOrderByApiModifiedTimeDesc(OPERATION);

        Map<String, TourContentRaw> latestByContentId = new LinkedHashMap<>();
        for (TourContentRaw raw : sortedDesc) {
            latestByContentId.putIfAbsent(raw.getContentId(), raw);
        }

        return List.copyOf(latestByContentId.values());
    }

    private static List<List<TourContentRaw>> partition(List<TourContentRaw> items, int size) {
        List<List<TourContentRaw>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            chunks.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return chunks;
    }
}
