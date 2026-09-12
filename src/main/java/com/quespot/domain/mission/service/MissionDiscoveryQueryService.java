package com.quespot.domain.mission.service;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.cursor.MissionListCursor;
import com.quespot.domain.mission.cursor.MissionListCursorCodec;
import com.quespot.domain.mission.dto.res.MissionDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionDiscoveryQueryRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MissionDiscoveryQueryService {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MissionRepository missionRepository;
    private final MissionDiscoveryQueryRepository missionDiscoveryQueryRepository;
    private final MissionListCursorCodec missionListCursorCodec;
    private final MissionAttemptStatusResolver missionAttemptStatusResolver;

    // 미션 목록 조회 로직
    @Transactional(readOnly = true)
    public MissionListResponseDTO getMissions(
            Long userId,
            MissionCategory category,
            String keyword,
            BigDecimal latitude,
            BigDecimal longitude,
            String cursorValue,
            int size
    ) {
        boolean hasLocation = validateLocation(latitude, longitude);
        String normalizedKeyword = normalizeKeyword(keyword);
        MissionListCursor.SortMode sortMode = hasLocation
                ? MissionListCursor.SortMode.DISTANCE
                : MissionListCursor.SortMode.RANDOM;
        String querySignature = missionListCursorCodec.querySignature(
                userId,
                category,
                normalizedKeyword,
                latitude,
                longitude
        );
        MissionListCursor cursor = resolveCursor(cursorValue, sortMode, querySignature);
        long seed = cursor == null
                ? initialSeed(sortMode, userId)
                : cursor.seed();

        List<MissionListProjection> rows = findMissions(
                category,
                normalizedKeyword,
                latitude,
                longitude,
                cursor,
                seed,
                size + 1
        );
        boolean hasNext = rows.size() > size;
        List<MissionListProjection> page = hasNext ? rows.subList(0, size) : rows;
        Map<Long, UserMissionStatus> statusByMissionId = missionAttemptStatusResolver.resolveStatuses(
                userId, page.stream().map(MissionListProjection::getMissionId).toList()
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

    // 미션 상세 조회 로직
    @Transactional(readOnly = true)
    public MissionDetailResponseDTO getMission(
            Long userId,
            Long missionId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        boolean hasLocation = validateLocation(latitude, longitude);
        Mission mission = missionRepository.findByIdAndStatus(missionId, MissionStatus.ACTIVE)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));
        Long distanceMeters = hasLocation
                ? calculateDistance(
                        latitude,
                        longitude,
                        mission.getSnapshotLatitude(),
                        mission.getSnapshotLongitude()
                )
                : null;

        return MissionConverter.toMissionDetail(
                mission,
                distanceMeters,
                missionAttemptStatusResolver.resolveStatus(userId, missionId)
        );
    }

    private List<MissionListProjection> findMissions(
            MissionCategory category,
            String keyword,
            BigDecimal latitude,
            BigDecimal longitude,
            MissionListCursor cursor,
            long seed,
            int limit
    ) {
        String categoryValue = category == null ? null : category.name();
        Double cursorSortValue = cursor == null ? null : cursor.sortValue();
        Long cursorMissionId = cursor == null ? null : cursor.missionId();

        if (latitude != null) {
            return missionDiscoveryQueryRepository.findMissionListByDistance(
                    categoryValue,
                    keyword,
                    latitude,
                    longitude,
                    cursorSortValue,
                    cursorMissionId,
                    limit
            );
        }
        return missionDiscoveryQueryRepository.findMissionListRandomly(
                categoryValue,
                keyword,
                seed,
                cursorSortValue,
                cursorMissionId,
                limit
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

    private boolean validateLocation(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new MissionException(MissionErrorCode.INVALID_LOCATION);
        }
        if (latitude == null) {
            return false;
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0
                || latitude.compareTo(MAX_LATITUDE) > 0
                || longitude.compareTo(MIN_LONGITUDE) < 0
                || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new MissionException(MissionErrorCode.INVALID_LOCATION);
        }
        return true;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.strip()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private long dailyRandomSeed(Long userId) {
        return 31 * userId + LocalDate.now(SERVICE_ZONE_ID).toEpochDay();
    }

    private long initialSeed(MissionListCursor.SortMode sortMode, Long userId) {
        return sortMode == MissionListCursor.SortMode.RANDOM ? dailyRandomSeed(userId) : 0;
    }

    private long calculateDistance(
            BigDecimal sourceLatitude,
            BigDecimal sourceLongitude,
            BigDecimal targetLatitude,
            BigDecimal targetLongitude
    ) {
        double sourceLatRadians = Math.toRadians(sourceLatitude.doubleValue());
        double targetLatRadians = Math.toRadians(targetLatitude.doubleValue());
        double latitudeDelta = targetLatRadians - sourceLatRadians;
        double longitudeDelta = Math.toRadians(
                targetLongitude.subtract(sourceLongitude).doubleValue()
        );
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(sourceLatRadians)
                * Math.cos(targetLatRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double centralAngle = 2 * Math.asin(Math.min(1, Math.sqrt(haversine)));
        return Math.round(EARTH_RADIUS_METERS * centralAngle);
    }
}
