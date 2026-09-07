package com.quespot.domain.spot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import com.quespot.domain.tour.client.dto.TourSyncItem;
import com.quespot.domain.tour.entity.TourContentRaw;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SpotRefinementWriter {

    // 대한민국 대략적인 bbox. 이탈 시 저장은 정상적으로 하되 WARN만 남긴다 —
    // 원천에 경위도가 뒤바뀐 건이 섞여 있는데, 걸러내지 않으면 GPS 인증이
    // 영원히 실패하는 미션이 된다.
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("33.0");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("38.7");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("124.5");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("132.0");

    private final SpotRepository spotRepository;
    private final CategoryResolver categoryResolver;
    private final ObjectMapper objectMapper;

    public SpotRefinementWriter(
            SpotRepository spotRepository,
            CategoryResolver categoryResolver,
            ObjectMapper objectMapper
    ) {
        this.spotRepository = spotRepository;
        this.categoryResolver = categoryResolver;
        this.objectMapper = objectMapper;
    }

    // 청크(페이지) 단위 커밋. 한 건이 실패해도 나머지는 계속 처리하고 실패 건은
    // 로그로 남긴다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefinementSummary refineChunk(List<TourContentRaw> rawRows) {
        int created = 0;
        int updated = 0;
        int skippedInvalidCoordinates = 0;
        int skippedUnchanged = 0;
        int failed = 0;

        for (TourContentRaw raw : rawRows) {
            try {
                RefinementResult result = refineOne(raw);
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

    private RefinementResult refineOne(TourContentRaw raw) throws JsonProcessingException {
        TourSyncItem item = objectMapper.readValue(raw.getPayload(), TourSyncItem.class);

        Optional<Spot> existingOpt =
                spotRepository.findBySourceAndSourceContentId(SpotSource.TOUR_API, raw.getContentId());

        if (existingOpt.isPresent() && raw.getApiModifiedTime().equals(existingOpt.get().getSourceModifiedAt())) {
            return RefinementResult.SKIPPED_UNCHANGED;
        }

        BigDecimal longitude = parseCoordinate(item.mapx());
        BigDecimal latitude = parseCoordinate(item.mapy());
        if (longitude == null || latitude == null) {
            // (0,0) 같은 가짜 좌표를 넣지 않는다 — 기니만 앞바다의 실재 좌표라
            // "좌표 없음"과 "정말 0,0"을 구분할 수 없게 된다. 원본은
            // tour_contents_raw에 그대로 있으니 나중에 좌표가 채워지면 재정제된다.
            log.warn(
                    "좌표 파싱 실패로 스킵: contentId={}, mapx={}, mapy={}",
                    raw.getContentId(), item.mapx(), item.mapy()
            );
            return RefinementResult.SKIPPED_INVALID_COORDINATES;
        }

        if (isOutsideKoreaBbox(latitude, longitude)) {
            log.warn("좌표 bbox 이탈. contentId={}, lat={}, lng={}", raw.getContentId(), latitude, longitude);
        }

        Spot freshData = Spot.builder()
                .source(SpotSource.TOUR_API)
                .sourceContentId(raw.getContentId())
                .name(item.title())
                .address(combineAddress(item.addr1(), item.addr2()))
                .latitude(latitude)
                .longitude(longitude)
                .imageUrl(item.firstimage())
                .thumbnailUrl(item.firstimage2())
                .ktoContentTypeId(parseIntOrNull(item.contenttypeid()))
                .lclsSystm1(item.lclsSystm1())
                .lclsSystm2(item.lclsSystm2())
                .lclsSystm3(item.lclsSystm3())
                .ldongRegnCd(item.lDongRegnCd())
                .ldongSignguCd(item.lDongSignguCd())
                .cpyrhtDivCd(item.cpyrhtDivCd())
                .appCategory(categoryResolver.resolve(item.lclsSystm1(), item.lclsSystm2(), item.lclsSystm3()))
                .categoryMappingVersion(categoryResolver.getCurrentVersion())
                .showFlag("1".equals(item.showflag()))
                .sourceModifiedAt(raw.getApiModifiedTime())
                .build();

        if (existingOpt.isPresent()) {
            Spot existing = existingOpt.get();
            existing.refresh(freshData);
            spotRepository.saveAndFlush(existing);
            return RefinementResult.UPDATED;
        }

        spotRepository.saveAndFlush(freshData);
        return RefinementResult.CREATED;
    }

    private BigDecimal parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isOutsideKoreaBbox(BigDecimal latitude, BigDecimal longitude) {
        return latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0
                || longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0;
    }

    private String combineAddress(String addr1, String addr2) {
        String base = addr1 == null ? "" : addr1;
        String detail = addr2 == null ? "" : addr2;
        String combined = (detail.isBlank() ? base : base + " " + detail).trim();
        return combined.isBlank() ? null : combined;
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
