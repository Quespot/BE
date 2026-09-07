package com.quespot.domain.tour;

import com.fasterxml.jackson.databind.JsonNode;
import com.quespot.domain.tour.client.TourApiClient;
import com.quespot.domain.tour.client.dto.TourApiResponse;
import com.quespot.domain.tour.service.RawPersister;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

// 정제 배치 개발용 샘플 데이터를 실제 TourAPI에서 확보하는 "수동 실행 전용" 도구다.
// 서비스키는 일 1,000건 제한이고 로컬 호출도 그대로 차감되므로 절대 자동으로
// 돌면 안 된다. 실행하려면:
//   1) 아래 @Disabled를 잠깐 지우거나 주석 처리한다
//   2) 이 테스트 하나만 돌린다:
//      ./gradlew test --tests "com.quespot.domain.tour.TourApiSampleDataFetcherManualTest" --info
//   3) 결과 확인 후 반드시 @Disabled를 원상복구하고 커밋한다
//
// @Transactional을 이 클래스에 붙이지 않는다 — 붙이면 테스트 종료 시 자동
// 롤백되어 tour_contents_raw에 아무것도 안 남는다. RawPersister.saveAll이
// 자체적으로 REQUIRES_NEW로 독립 커밋하므로 여기선 손대지 않아도 된다.
//
// 콜은 정확히 3번(contentTypeId 12/14/39, lDongRegnCd=11, numOfRows=20, pageNo=1)만
// 나가야 해서 반복문을 쓰지 않고 세 블록을 그대로 나열한다 — 나중에 누가 리스트로
// 바꾸다가 실수로 호출을 늘리는 걸 막기 위함이다. arrange=R, showflag=1은
// TourApiClient에 이미 고정 파라미터로 들어가 있어 여기서 따로 넘기지 않는다.
@SpringBootTest
@Disabled("실제 TourAPI 호출 3회 + DB 저장 — 쿼터를 소비하므로 수동 실행 전용")
class TourApiSampleDataFetcherManualTest {

    private static final Logger log = LoggerFactory.getLogger(TourApiSampleDataFetcherManualTest.class);

    private static final String LDONG_REGN_CD = "11";
    private static final int NUM_OF_ROWS = 20;
    private static final int PAGE_NO = 1;
    private static final String OPERATION = "areaBasedSyncList2";

    @Autowired
    private TourApiClient tourApiClient;

    @Autowired
    private RawPersister rawPersister;

    @Test
    void fetchAndSaveThreeContentTypeSamples() {
        // contentTypeId 12 (관광지) — 1콜
        TourApiResponse<JsonNode> tourSpotResponse =
                tourApiClient.fetchSyncList(LDONG_REGN_CD, 12, PAGE_NO, NUM_OF_ROWS, null);
        logAndSave(12, tourSpotResponse);

        // contentTypeId 14 (문화시설) — 1콜
        TourApiResponse<JsonNode> cultureFacilityResponse =
                tourApiClient.fetchSyncList(LDONG_REGN_CD, 14, PAGE_NO, NUM_OF_ROWS, null);
        logAndSave(14, cultureFacilityResponse);

        // contentTypeId 39 (음식점) — 1콜
        TourApiResponse<JsonNode> restaurantResponse =
                tourApiClient.fetchSyncList(LDONG_REGN_CD, 39, PAGE_NO, NUM_OF_ROWS, null);
        logAndSave(39, restaurantResponse);
    }

    private void logAndSave(int contentTypeId, TourApiResponse<JsonNode> response) {
        List<JsonNode> items = response.response().body().items();
        long totalCount = response.response().body().totalCount();

        log.info("contentTypeId={} totalCount={} items.size={}", contentTypeId, totalCount, items.size());
        if (!items.isEmpty()) {
            log.info("contentTypeId={} firstItem={}", contentTypeId, items.get(0));
        }

        rawPersister.saveAll(OPERATION, items);
    }
}
