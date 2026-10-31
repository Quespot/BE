package com.quespot.domain.spot.service;

import com.quespot.domain.tour.entity.TourContentRaw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SpotRefinementWriter {

    private final SpotRowWriter spotRowWriter;

    public SpotRefinementWriter(SpotRowWriter spotRowWriter) {
        this.spotRowWriter = spotRowWriter;
    }

    // 청크(페이지) 단위로 묶어 처리하되, 건별로 별도 물리 트랜잭션
    // (SpotRowWriter.refineOne)에 커밋한다 — 한 건이 실패해도 그 건의
    // 트랜잭션만 롤백되고 나머지 건은 영향받지 않는다.
    public RefinementSummary refineChunk(List<TourContentRaw> rawRows) {
        int created = 0;
        int updated = 0;
        int skippedInvalidCoordinates = 0;
        int skippedUnchanged = 0;
        int failed = 0;

        for (TourContentRaw raw : rawRows) {
            try {
                RefinementResult result = spotRowWriter.refineOne(raw);
                switch (result) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case SKIPPED_INVALID_COORDINATES -> skippedInvalidCoordinates++;
                    case SKIPPED_UNCHANGED -> skippedUnchanged++;
                }
            } catch (Exception e) {
                failed++;
                log.error("정제 실패: contentId={}", raw.getContentId(), e);
            }
        }

        return new RefinementSummary(created, updated, skippedInvalidCoordinates, skippedUnchanged, failed);
    }
}
