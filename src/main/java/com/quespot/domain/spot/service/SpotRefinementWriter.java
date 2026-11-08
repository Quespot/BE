package com.quespot.domain.spot.service;

import com.quespot.domain.tour.entity.TourContentRaw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SpotRefinementWriter {

    private final SpotRowWriter spotRowWriter;

    public SpotRefinementWriter(SpotRowWriter spotRowWriter) {
        this.spotRowWriter = spotRowWriter;
    }

    // 청크를 하나의 트랜잭션(SpotRowWriter.refineAll)으로 먼저 정제한다. 청크 안에서
    // 한 건이라도 실패하면 그 청크만 롤백하고, 건별 트랜잭션(SpotRowWriter.refineOne)으로
    // 다시 처리해 실패한 건만 걸러낸다 — 한 건의 실패가 나머지 건에 전염되지 않는다.
    public RefinementSummary refineChunk(List<TourContentRaw> rawRows) {
        try {
            return spotRowWriter.refineAll(rawRows);
        } catch (Exception e) {
            log.warn("청크 일괄 정제 실패, 건별 정제로 재시도: size={}, cause={}", rawRows.size(), e.toString());
            return refineEach(rawRows);
        }
    }

    private RefinementSummary refineEach(List<TourContentRaw> rawRows) {
        List<RefinementResult> results = new ArrayList<>(rawRows.size());
        int failed = 0;
        for (TourContentRaw raw : rawRows) {
            try {
                results.add(spotRowWriter.refineOne(raw));
            } catch (Exception e) {
                failed++;
                log.error("정제 실패: contentId={}", raw.getContentId(), e);
            }
        }
        return RefinementSummary.of(results, failed);
    }
}
