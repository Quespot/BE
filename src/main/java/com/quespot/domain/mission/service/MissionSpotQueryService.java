package com.quespot.domain.mission.service;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.cursor.MissionListCursor;
import com.quespot.domain.mission.cursor.MissionListCursorCodec;
import com.quespot.domain.mission.dto.res.MissionListItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionSpotItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionSpotMapResponseDTO;
import com.quespot.domain.mission.dto.res.NearbyMissionSpotListResponseDTO;
import com.quespot.domain.mission.enums.MissionSpotCompletionStatus;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionSpotQueryRepository;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import com.quespot.domain.mission.repository.projection.MissionSpotSummaryProjection;
import com.quespot.domain.mission.repository.projection.NearbyMissionSpotProjection;
import com.quespot.domain.spot.model.AdministrativeDistrict;
import com.quespot.domain.spot.service.AdministrativeDistrictResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MissionSpotQueryService {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MissionSpotQueryRepository missionSpotQueryRepository;
    private final AdministrativeDistrictResolver administrativeDistrictResolver;
    private final MissionListCursorCodec missionListCursorCodec;
    private final MissionAttemptStatusResolver missionAttemptStatusResolver;

    // 행정구역 미션 스팟 조회 로직
    @Transactional(readOnly = true)
    public MissionSpotMapResponseDTO getMissionSpots(Long userId, String regionCode) {
        List<AdministrativeDistrict> districts = administrativeDistrictResolver.findByRegionCode(regionCode);
        if (districts.isEmpty()) {
            throw new MissionException(MissionErrorCode.REGION_NOT_SUPPORTED);
        }

        Map<String, AdministrativeDistrict> districtByCode = districts.stream()
                .collect(java.util.stream.Collectors.toMap(
                        AdministrativeDistrict::districtCode,
                        district -> district
                ));
        List<MissionSpotItemResponseDTO> missionSpots = missionSpotQueryRepository
                .findMissionSpotSummaries(userId, regionCode)
                .stream()
                .filter(row -> districtByCode.containsKey(row.getDistrictCode()))
                .map(row -> toItem(row, districtByCode.get(row.getDistrictCode()), null))
                .toList();

        return new MissionSpotMapResponseDTO(
                regionCode,
                districts.get(0).regionName(),
                missionSpots.size(),
                (int) missionSpots.stream().filter(this::isCompleted).count(),
                missionSpots.stream().mapToLong(MissionSpotItemResponseDTO::missionCount).sum(),
                missionSpots.stream().mapToLong(MissionSpotItemResponseDTO::completedMissionCount).sum(),
                missionSpots
        );
    }

    // 주변 미션 스팟 조회 로직
    @Transactional(readOnly = true)
    public NearbyMissionSpotListResponseDTO getNearbyMissionSpots(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            int limit
    ) {
        validateRequiredLocation(latitude, longitude);
        List<MissionSpotItemResponseDTO> missionSpots = missionSpotQueryRepository
                .findNearbyMissionSpots(userId, latitude, longitude, limit)
                .stream()
                .map(this::toNearbyItem)
                .flatMap(Optional::stream)
                .toList();
        return new NearbyMissionSpotListResponseDTO(missionSpots);
    }

    // 행정구역 미션 목록 조회 로직
    @Transactional(readOnly = true)
    public MissionListResponseDTO getDistrictMissions(
            Long userId,
            String districtCode,
            BigDecimal latitude,
            BigDecimal longitude,
            String cursorValue,
            int size
    ) {
        administrativeDistrictResolver.findByCode(districtCode)
                .orElseThrow(() -> new MissionException(MissionErrorCode.DISTRICT_NOT_FOUND));
        boolean hasLocation = validateOptionalLocation(latitude, longitude);
        MissionListCursor.SortMode sortMode = hasLocation
                ? MissionListCursor.SortMode.DISTANCE
                : MissionListCursor.SortMode.RANDOM;
        String querySignature = missionListCursorCodec.querySignature(
                userId,
                null,
                "district:" + districtCode,
                latitude,
                longitude
        );
        MissionListCursor cursor = resolveCursor(cursorValue, sortMode, querySignature);
        long seed = cursor == null
                ? initialSeed(sortMode, userId)
                : cursor.seed();

        List<MissionListProjection> rows = findDistrictMissions(
                districtCode,
                latitude,
                longitude,
                cursor,
                seed,
                size + 1
        );
        boolean hasNext = rows.size() > size;
        List<MissionListProjection> page = hasNext ? rows.subList(0, size) : rows;
        Map<Long, UserMissionStatus> statusByMissionId = missionAttemptStatusResolver.resolveStatuses(
                userId,
                page.stream().map(MissionListProjection::getMissionId).toList()
        );
        List<MissionListItemResponseDTO> missions = page.stream()
                .map(row -> MissionConverter.toMissionListItem(
                        row,
                        statusByMissionId.getOrDefault(row.getMissionId(), UserMissionStatus.AVAILABLE),
                        hasLocation
                ))
                .toList();
        String nextCursor = hasNext
                ? nextCursor(page.get(page.size() - 1), sortMode, seed, querySignature)
                : null;

        return new MissionListResponseDTO(missions, nextCursor, hasNext);
    }

    private List<MissionListProjection> findDistrictMissions(
            String districtCode,
            BigDecimal latitude,
            BigDecimal longitude,
            MissionListCursor cursor,
            long seed,
            int limit
    ) {
        Double cursorSortValue = cursor == null ? null : cursor.sortValue();
        Long cursorMissionId = cursor == null ? null : cursor.missionId();
        if (latitude != null) {
            return missionSpotQueryRepository.findDistrictMissionsByDistance(
                    districtCode,
                    latitude,
                    longitude,
                    cursorSortValue,
                    cursorMissionId,
                    limit
            );
        }
        return missionSpotQueryRepository.findDistrictMissionsRandomly(
                districtCode,
                seed,
                cursorSortValue,
                cursorMissionId,
                limit
        );
    }

    private Optional<MissionSpotItemResponseDTO> toNearbyItem(NearbyMissionSpotProjection row) {
        return administrativeDistrictResolver.findByCode(row.getDistrictCode())
                .map(district -> {
                    Long distanceMeters = row.getDistanceMeters() == null
                            ? null
                            : Math.round(row.getDistanceMeters());
                    return toItem(row, district, distanceMeters);
                });
    }

    private MissionSpotItemResponseDTO toItem(
            MissionSpotSummaryProjection row,
            AdministrativeDistrict district,
            Long distanceMeters
    ) {
        long missionCount = row.getMissionCount();
        long completedMissionCount = row.getCompletedMissionCount();
        MissionSpotCompletionStatus completionStatus = missionCount > 0
                && missionCount == completedMissionCount
                ? MissionSpotCompletionStatus.COMPLETED
                : MissionSpotCompletionStatus.INCOMPLETE;
        return new MissionSpotItemResponseDTO(
                district.districtCode(),
                district.districtName(),
                district.centerLatitude(),
                district.centerLongitude(),
                missionCount,
                completedMissionCount,
                completionStatus,
                distanceMeters
        );
    }

    private MissionListCursor resolveCursor(
            String cursorValue,
            MissionListCursor.SortMode sortMode,
            String querySignature
    ) {
        if (cursorValue == null || cursorValue.isBlank()) {
            return null;
        }
        MissionListCursor cursor = missionListCursorCodec.decode(cursorValue);
        if (cursor.sortMode() != sortMode
                || (sortMode == MissionListCursor.SortMode.DISTANCE && cursor.seed() != 0)
                || !cursor.querySignature().equals(querySignature)) {
            throw new MissionException(MissionErrorCode.INVALID_CURSOR);
        }
        return cursor;
    }

    private String nextCursor(
            MissionListProjection lastMission,
            MissionListCursor.SortMode sortMode,
            long seed,
            String querySignature
    ) {
        return missionListCursorCodec.encode(new MissionListCursor(
                sortMode,
                seed,
                lastMission.getSortValue(),
                lastMission.getMissionId(),
                querySignature
        ));
    }

    private boolean validateOptionalLocation(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new MissionException(MissionErrorCode.INVALID_LOCATION);
        }
        if (latitude == null) {
            return false;
        }
        validateRequiredLocation(latitude, longitude);
        return true;
    }

    private void validateRequiredLocation(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null
                || longitude == null
                || latitude.compareTo(MIN_LATITUDE) < 0
                || latitude.compareTo(MAX_LATITUDE) > 0
                || longitude.compareTo(MIN_LONGITUDE) < 0
                || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new MissionException(MissionErrorCode.INVALID_LOCATION);
        }
    }

    private boolean isCompleted(MissionSpotItemResponseDTO item) {
        return item.completionStatus() == MissionSpotCompletionStatus.COMPLETED;
    }

    private long initialSeed(MissionListCursor.SortMode sortMode, Long userId) {
        if (sortMode == MissionListCursor.SortMode.DISTANCE) {
            return 0;
        }
        return 31 * userId + LocalDate.now(SERVICE_ZONE_ID).toEpochDay();
    }
}
