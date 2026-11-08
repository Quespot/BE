package com.quespot.domain.spot.service;

import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// @Transactional을 걸지 않는다 — 청크별 독립 커밋(SpotRefinementWriter)을 위해서다.
@Slf4j
@Component
public class SpotRefiner {

    private static final String OPERATION = "areaBasedSyncList2";
    private static final int CHUNK_SIZE = 200;

    private final TourContentRawRepository tourContentRawRepository;
    private final SpotRefinementWriter spotRefinementWriter;
    private final CategoryResolver categoryResolver;

    public SpotRefiner(
            TourContentRawRepository tourContentRawRepository,
            SpotRefinementWriter spotRefinementWriter,
            CategoryResolver categoryResolver
    ) {
        this.tourContentRawRepository = tourContentRawRepository;
        this.spotRefinementWriter = spotRefinementWriter;
        this.categoryResolver = categoryResolver;
    }

    public RefinementSummary refine() {
        // content_id별 최신 원본 중 spot과 달라진 것만 ID로 고르고, payload는 청크 단위로 읽는다.
        // 원본 전체를 메모리에 올리지 않고, 변경 없는 원본은 트랜잭션도 열지 않는다.
        List<Long> targetIds = tourContentRawRepository.findRefinementTargetIds(
                OPERATION,
                categoryResolver.getCurrentVersion()
        );
        long contentCount = tourContentRawRepository.countContents(OPERATION);

        List<RefinementSummary> chunkResults = new ArrayList<>();
        for (List<Long> chunkIds : partition(targetIds, CHUNK_SIZE)) {
            List<TourContentRaw> chunk = tourContentRawRepository.findAllById(chunkIds);
            chunkResults.add(spotRefinementWriter.refineChunk(chunk));
        }

        int unchangedCount = (int) (contentCount - targetIds.size());
        RefinementSummary summary = RefinementSummary.combine(chunkResults).withAdditionalUnchanged(unchangedCount);
        log.info(
                "정제 완료: 대상 {}건(변경 {}건), 생성 {}건, 갱신 {}건, 좌표없음스킵 {}건, 변경없음스킵 {}건, 실패 {}건",
                contentCount, targetIds.size(), summary.created(), summary.updated(),
                summary.skippedInvalidCoordinates(), summary.skippedUnchanged(), summary.failed()
        );
        return summary;
    }

    private static <T> List<List<T>> partition(List<T> items, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            chunks.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return chunks;
    }
}
