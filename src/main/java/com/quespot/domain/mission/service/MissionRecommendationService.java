package com.quespot.domain.mission.service;

import com.quespot.domain.like.enums.LikeTargetType;
import com.quespot.domain.like.repository.LikeRepository;
import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.res.RecommendedMissionItemResponseDTO;
import com.quespot.domain.mission.dto.res.RecommendedMissionListResponseDTO;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.repository.projection.RecommendedMissionProjection;
import com.quespot.domain.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionRecommendationService {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int MIN_SCAN_BATCH_SIZE = 30;
    private static final int MAX_SCAN_BATCH_SIZE = 300;

    private final MissionRepository missionRepository;
    private final UserProfileRepository userProfileRepository;
    private final MissionAttemptStatusResolver missionAttemptStatusResolver;
    private final LikeRepository likeRepository;
    private final RecommendationCursorCodec recommendationCursorCodec;

    // 추천 미션 목록 조회 로직
    @Transactional(readOnly = true)
    public RecommendedMissionListResponseDTO getRecommendations(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            String cursorValue,
            int size
    ) {
        boolean hasLocation = validateLocation(latitude, longitude);
        String preferredCategories = preferredCategories(userId);
        long seed = dailySeed(userId);
        RecommendationCursor.SortMode sortMode = hasLocation
                ? RecommendationCursor.SortMode.DISTANCE
                : RecommendationCursor.SortMode.RANDOM;
        String querySignature = recommendationCursorCodec.querySignature(
                userId, preferredCategories, latitude, longitude
        );
        RecommendationCursor requestedCursor = resolveCursor(
                cursorValue, sortMode, seed, querySignature
        );

        List<RecommendedMissionProjection> availableRows = findAvailableRows(
                userId,
                preferredCategories,
                latitude,
                longitude,
                sortMode,
                seed,
                querySignature,
                requestedCursor,
                size + 1
        );
        boolean hasNext = availableRows.size() > size;
        List<RecommendedMissionProjection> page = hasNext
                ? availableRows.subList(0, size)
                : availableRows;
        Set<Long> likedMissionIds = findLikedMissionIds(userId, page);
        List<RecommendedMissionItemResponseDTO> missions = page.stream()
                .map(row -> MissionConverter.toRecommendedMissionItem(
                        row, hasLocation, likedMissionIds.contains(row.getMissionId())
                ))
                .toList();
        String nextCursor = hasNext
                ? recommendationCursorCodec.encode(toCursor(
                        page.get(page.size() - 1), sortMode, seed, querySignature
                ))
                : null;

        return new RecommendedMissionListResponseDTO(missions, nextCursor, hasNext);
    }

    private List<RecommendedMissionProjection> findAvailableRows(
            Long userId,
            String preferredCategories,
            BigDecimal latitude,
            BigDecimal longitude,
            RecommendationCursor.SortMode sortMode,
            long seed,
            String querySignature,
            RecommendationCursor requestedCursor,
            int requiredCount
    ) {
        int batchSize = Math.min(
                MAX_SCAN_BATCH_SIZE,
                Math.max(MIN_SCAN_BATCH_SIZE, requiredCount * 3)
        );
        List<RecommendedMissionProjection> availableRows = new ArrayList<>(requiredCount);
        RecommendationCursor scanCursor = requestedCursor;

        while (availableRows.size() < requiredCount) {
            List<RecommendedMissionProjection> rows = missionRepository.findRecommendedMissions(
                    userId,
                    preferredCategories,
                    latitude,
                    longitude,
                    seed,
                    scanCursor == null ? null : scanCursor.preferenceRank(),
                    scanCursor == null ? null : scanCursor.categoryRank(),
                    scanCursor == null ? null : scanCursor.categoryOrder(),
                    scanCursor == null ? null : scanCursor.sortValue(),
                    scanCursor == null ? null : scanCursor.missionId(),
                    batchSize
            );
            if (rows.isEmpty()) {
                break;
            }

            var statuses = missionAttemptStatusResolver.resolveStatuses(
                    userId, rows.stream().map(RecommendedMissionProjection::getMissionId).toList()
            );
            for (RecommendedMissionProjection row : rows) {
                if (statuses.getOrDefault(row.getMissionId(), UserMissionStatus.AVAILABLE)
                        == UserMissionStatus.AVAILABLE) {
                    availableRows.add(row);
                    if (availableRows.size() == requiredCount) {
                        break;
                    }
                }
            }

            if (availableRows.size() == requiredCount || rows.size() < batchSize) {
                break;
            }
            scanCursor = toCursor(
                    rows.get(rows.size() - 1), sortMode, seed, querySignature
            );
        }
        return availableRows;
    }

    private Set<Long> findLikedMissionIds(Long userId, List<RecommendedMissionProjection> page) {
        if (page.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(likeRepository.findLikedTargetIds(
                userId,
                LikeTargetType.MISSION,
                page.stream().map(RecommendedMissionProjection::getMissionId).toList()
        ));
    }

    private String preferredCategories(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> profile.getTravelStyles().stream()
                        .map(style -> style.toMissionCategory().name())
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(",")))
                .orElse("");
    }

    private RecommendationCursor resolveCursor(
            String cursorValue,
            RecommendationCursor.SortMode sortMode,
            long seed,
            String querySignature
    ) {
        if (cursorValue == null || cursorValue.isBlank()) {
            return null;
        }
        RecommendationCursor cursor = recommendationCursorCodec.decode(cursorValue);
        if (cursor.sortMode() != sortMode
                || cursor.seed() != seed
                || !cursor.querySignature().equals(querySignature)) {
            throw new MissionException(MissionErrorCode.INVALID_CURSOR);
        }
        return cursor;
    }

    private RecommendationCursor toCursor(
            RecommendedMissionProjection row,
            RecommendationCursor.SortMode sortMode,
            long seed,
            String querySignature
    ) {
        return new RecommendationCursor(
                sortMode,
                seed,
                row.getPreferenceRank(),
                row.getCategoryRank(),
                row.getCategoryOrder(),
                row.getSortValue(),
                row.getMissionId(),
                querySignature
        );
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

    private long dailySeed(Long userId) {
        return 31 * userId + LocalDate.now(SERVICE_ZONE_ID).toEpochDay();
    }
}
