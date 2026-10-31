package com.quespot.domain.tour.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.quespot.domain.tour.client.TourApiClient;
import com.quespot.domain.tour.client.TourApiException;
import com.quespot.domain.tour.client.dto.TourApiResponse;
import com.quespot.domain.tour.entity.SyncCheckpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// @Transactional을 걸지 않는다 — 외부 HTTP가 수백ms~수초인데 그동안 커넥션을
// 잡으면 풀이 마른다. 실제 DB 쓰기는 QuotaGuard/RawPersister/CheckpointWriter로
// 위임하고 각각 REQUIRES_NEW로 독립 커밋한다.
@Slf4j
@Component
public class TourContentCollector {

    private static final String OPERATION = "areaBasedSyncList2";

    // 서울. 확장 시 여기에 추가.
    private static final List<String> TARGET_REGIONS = List.of("11");

    // 12 관광지 / 14 문화시설 / 28 레포츠 / 38 쇼핑 / 39 음식점.
    // 15(축제)는 기간 만료 처리가 별도로 필요해 제외.
    // 25(여행코스)·32(숙박)은 미션 대상이 아님.
    private static final List<Integer> TARGET_CONTENT_TYPES = List.of(12, 14, 28, 38, 39);

    private static final int NUM_OF_ROWS = 100;

    private final TourApiClient tourApiClient;
    private final QuotaGuard quotaGuard;
    private final RawPersister rawPersister;
    private final CheckpointWriter checkpointWriter;

    public TourContentCollector(
            TourApiClient tourApiClient,
            QuotaGuard quotaGuard,
            RawPersister rawPersister,
            CheckpointWriter checkpointWriter
    ) {
        this.tourApiClient = tourApiClient;
        this.quotaGuard = quotaGuard;
        this.rawPersister = rawPersister;
        this.checkpointWriter = checkpointWriter;
    }

    public void collect() {
        for (String regionCode : TARGET_REGIONS) {
            for (Integer contentTypeId : TARGET_CONTENT_TYPES) {
                try {
                    collectOne(regionCode, contentTypeId);
                } catch (Exception e) {
                    log.error("TourAPI 수집 실패: region={}, contentTypeId={}", regionCode, contentTypeId, e);
                    checkpointWriter.fail(regionCode, contentTypeId);
                }
            }
        }
    }

    private void collectOne(String ldongRegnCd, Integer contentTypeId) {
        LocalDateTime candidateModifiedTime = LocalDate.now().minusDays(1).atStartOfDay();
        SyncCheckpoint checkpoint = checkpointWriter.loadOrStart(ldongRegnCd, contentTypeId, candidateModifiedTime);

        int page = checkpoint.nextPageNo();
        LocalDateTime modifiedTime = checkpoint.getLastModifiedTime();

        while (true) {
            if (!quotaGuard.tryConsume()) {
                checkpointWriter.suspendByQuota(ldongRegnCd, contentTypeId);
                return;
            }

            TourApiResponse<JsonNode> response;
            try {
                response = tourApiClient.fetchSyncList(ldongRegnCd, contentTypeId, page, NUM_OF_ROWS, modifiedTime);
            } catch (TourApiException e) {
                if (e.isQuotaExceeded()) {
                    checkpointWriter.suspendByQuota(ldongRegnCd, contentTypeId);
                    return;
                }
                throw e;
            }

            List<JsonNode> items = response.response().body().items();
            if (items.isEmpty()) {
                break;
            }

            rawPersister.saveAll(OPERATION, items);
            checkpointWriter.advance(ldongRegnCd, contentTypeId, page);

            // totalCount로 페이지 수를 미리 계산하지 않는다 — 수집 중에 값이 변한다.
            // 반환 개수로만 종료를 판단한다.
            if (items.size() < NUM_OF_ROWS) {
                break;
            }
            page++;
        }

        checkpointWriter.complete(ldongRegnCd, contentTypeId);
    }
}
