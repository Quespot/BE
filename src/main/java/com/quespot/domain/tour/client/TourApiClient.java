package com.quespot.domain.tour.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.tour.client.dto.TourApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TourApiClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
    private static final String OPERATION = "areaBasedSyncList2";
    private static final DateTimeFormatter MODIFIED_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String SUCCESS_RESULT_CODE = "0000";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;

    public TourApiClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.tour-api.service-key}") String serviceKey
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    // areaBasedSyncList2 하나로 통일한다. areaBasedList2엔 showflag가 없어
    // 비표출 전환을 감지할 수 없다.
    public TourApiResponse<JsonNode> fetchSyncList(
            String ldongRegnCd,
            int contentTypeId,
            int pageNo,
            int numOfRows,
            LocalDateTime modifiedTime
    ) {
        URI uri = buildUri(ldongRegnCd, contentTypeId, pageNo, numOfRows, modifiedTime);

        // _type=json을 붙여도 쿼터 초과(22) 같은 게이트웨이 오류는 XML로 온다.
        // 곧바로 객체로 역직렬화하면 Jackson이 원인 모호하게 터진다.
        String rawBody = restClient.get().uri(uri).retrieve().body(String.class);

        if (rawBody != null && rawBody.stripLeading().startsWith("<")) {
            throw TourApiException.fromXml(rawBody);
        }

        TourApiResponse<JsonNode> response = readResponse(rawBody);

        if (!SUCCESS_RESULT_CODE.equals(response.response().header().resultCode())) {
            throw TourApiException.fromResultCode(
                    response.response().header().resultCode(),
                    response.response().header().resultMsg()
            );
        }

        return response;
    }

    private TourApiResponse<JsonNode> readResponse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<TourApiResponse<JsonNode>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("TourAPI 응답 파싱에 실패했습니다.", e);
        }
    }

    private URI buildUri(
            String ldongRegnCd,
            int contentTypeId,
            int pageNo,
            int numOfRows,
            LocalDateTime modifiedTime
    ) {
        // arrange=R: (1) 대표이미지 보유분만 반환해 페이징 수를 줄인다 (2) createdtime이
        // 불변이라 수집 도중 순서가 흔들려도 결과가 누락이 아니라 중복으로 남는다 —
        // 중복은 UNIQUE가 흡수하지만 누락은 복구할 방법이 없다.
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(BASE_URL + "/" + OPERATION)
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "Quespot")
                .queryParam("_type", "json")
                .queryParam("showflag", 1)
                .queryParam("arrange", "R")
                .queryParam("lDongRegnCd", ldongRegnCd)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows);

        if (modifiedTime != null) {
            builder.queryParam("modifiedtime", modifiedTime.format(MODIFIED_TIME_FORMAT));
        }

        // 이미 인코딩된 서비스키의 이중 인코딩을 막는다.
        return builder.build(true).toUri();
    }
}
