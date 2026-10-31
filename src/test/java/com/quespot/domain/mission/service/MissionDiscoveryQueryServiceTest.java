package com.quespot.domain.mission.service;

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
import com.quespot.domain.mission.repository.MissionDiscoveryQueryRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionDiscoveryQueryServiceTest {

    private static final String CURSOR_SECRET = "mission-cursor-test-secret";

    private MissionRepository missionRepository;
    private MissionDiscoveryQueryRepository missionDiscoveryQueryRepository;
    private MissionAttemptStatusResolver missionAttemptStatusResolver;
    private MissionDiscoveryQueryService missionDiscoveryQueryService;

    @BeforeEach
    void setUp() {
        missionRepository = mock(MissionRepository.class);
        missionDiscoveryQueryRepository = mock(MissionDiscoveryQueryRepository.class);
        missionAttemptStatusResolver = mock(MissionAttemptStatusResolver.class);
        when(missionAttemptStatusResolver.resolveStatuses(anyLong(), any())).thenReturn(java.util.Map.of());
        when(missionAttemptStatusResolver.resolveStatus(anyLong(), anyLong())).thenReturn(UserMissionStatus.AVAILABLE);
        missionDiscoveryQueryService = new MissionDiscoveryQueryService(
                missionRepository,
                missionDiscoveryQueryRepository,
                new MissionListCursorCodec(CURSOR_SECRET),
                missionAttemptStatusResolver
        );
    }

    @Test
    void returnsDistanceOrderedMissionPageWithinFirstRadius() {
        MissionListProjection first = projection(1L, MissionCategory.HISTORY, 1200.4);
        MissionListProjection second = projection(2L, MissionCategory.HISTORY, 2300.0);
        when(missionDiscoveryQueryRepository.findMissionListByDistanceWithinRadius(
                eq("HISTORY"),
                eq("경복궁"),
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                eq(5_000),
                isNull(),
                isNull(),
                eq(2)
        )).thenReturn(List.of(first, second));

        MissionListResponseDTO response = missionDiscoveryQueryService.getMissions(
                10L,
                MissionCategory.HISTORY,
                " 경복궁 ",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                null,
                1
        );

        assertThat(response.missions()).hasSize(1);
        assertThat(response.missions().get(0).distanceMeters()).isEqualTo(1200L);
        assertThat(response.missions().get(0).userMissionStatus()).isEqualTo(UserMissionStatus.AVAILABLE);
        assertThat(response.missions().get(0).canStart()).isTrue();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();
        verify(missionDiscoveryQueryRepository, never()).findMissionListByDistance(
                any(), any(), any(), any(), any(), any(), anyInt()
        );
    }

    @Test
    void expandsRadiusFromLastFoundMissionWhenPageIsNotFilled() {
        MissionListProjection near = projection(1L, MissionCategory.FOOD, 3_000.0);
        MissionListProjection middle = projection(2L, MissionCategory.FOOD, 15_000.0);
        MissionListProjection far = projection(3L, MissionCategory.FOOD, 120_000.0);
        when(missionDiscoveryQueryRepository.findMissionListByDistanceWithinRadius(
                isNull(), isNull(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(5_000), isNull(), isNull(), eq(3)
        )).thenReturn(List.of(near));
        when(missionDiscoveryQueryRepository.findMissionListByDistanceWithinRadius(
                isNull(), isNull(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(20_000), eq(3_000.0), eq(1L), eq(2)
        )).thenReturn(List.of(middle));
        when(missionDiscoveryQueryRepository.findMissionListByDistanceWithinRadius(
                isNull(), isNull(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(50_000), eq(15_000.0), eq(2L), eq(1)
        )).thenReturn(List.of());
        when(missionDiscoveryQueryRepository.findMissionListByDistance(
                isNull(), isNull(), any(), any(), eq(15_000.0), eq(2L), eq(1)
        )).thenReturn(List.of(far));

        MissionListResponseDTO response = missionDiscoveryQueryService.getMissions(
                10L,
                null,
                null,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                null,
                2
        );

        assertThat(response.missions()).extracting(MissionListItemResponseDTO::missionId)
                .containsExactly(1L, 2L);
        assertThat(response.hasNext()).isTrue();
        MissionListCursor cursor = new MissionListCursorCodec(CURSOR_SECRET).decode(response.nextCursor());
        assertThat(cursor.missionId()).isEqualTo(2L);
        assertThat(cursor.radiusStep()).isEqualTo(MissionSearchRadius.unlimitedStep());
    }

    @Test
    void continuesFromCursorRadiusStep() {
        MissionListCursorCodec codec = new MissionListCursorCodec(CURSOR_SECRET);
        BigDecimal latitude = new BigDecimal("37.5665");
        BigDecimal longitude = new BigDecimal("126.9780");
        String signature = codec.querySignature(10L, null, null, latitude, longitude);
        String cursor = codec.encode(new MissionListCursor(
                MissionListCursor.SortMode.DISTANCE,
                0,
                18_000.0,
                7L,
                1,
                signature
        ));
        MissionListProjection next = projection(8L, MissionCategory.NATURE, 19_000.0);
        MissionListProjection afterNext = projection(9L, MissionCategory.NATURE, 19_500.0);
        when(missionDiscoveryQueryRepository.findMissionListByDistanceWithinRadius(
                isNull(), isNull(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(20_000), eq(18_000.0), eq(7L), eq(2)
        )).thenReturn(List.of(next, afterNext));

        MissionListResponseDTO response = missionDiscoveryQueryService.getMissions(
                10L, null, null, latitude, longitude, cursor, 1
        );

        assertThat(response.missions()).extracting(MissionListItemResponseDTO::missionId).containsExactly(8L);
        verify(missionDiscoveryQueryRepository, never()).findMissionListByDistanceWithinRadius(
                any(), any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(5_000), any(), any(), anyInt()
        );
    }

    @Test
    void rejectsDistanceCursorWithUnknownRadiusStep() {
        MissionListCursorCodec codec = new MissionListCursorCodec(CURSOR_SECRET);
        BigDecimal latitude = new BigDecimal("37.5665");
        BigDecimal longitude = new BigDecimal("126.9780");
        String signature = codec.querySignature(10L, null, null, latitude, longitude);
        String cursor = codec.encode(new MissionListCursor(
                MissionListCursor.SortMode.DISTANCE,
                0,
                1_000.0,
                1L,
                MissionSearchRadius.unlimitedStep() + 1,
                signature
        ));

        assertThatThrownBy(() -> missionDiscoveryQueryService.getMissions(
                10L, null, null, latitude, longitude, cursor, 20
        )).isInstanceOf(MissionException.class);
    }

    @Test
    void returnsStableRandomOrderWithoutLocation() {
        MissionListProjection mission = projection(1L, MissionCategory.FOOD, 1234.0);
        when(missionDiscoveryQueryRepository.findMissionListRandomly(
                isNull(),
                isNull(),
                anyLong(),
                isNull(),
                isNull(),
                eq(21)
        )).thenReturn(List.of(mission));

        MissionListResponseDTO response = missionDiscoveryQueryService.getMissions(
                10L,
                null,
                null,
                null,
                null,
                null,
                20
        );

        assertThat(response.missions()).hasSize(1);
        assertThat(response.missions().get(0).distanceMeters()).isNull();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void rejectsCursorWhenSearchConditionChanges() {
        MissionListCursorCodec codec = new MissionListCursorCodec(CURSOR_SECRET);
        String signature = codec.querySignature(10L, MissionCategory.HISTORY, null, null, null);
        String cursor = codec.encode(new MissionListCursor(
                MissionListCursor.SortMode.RANDOM,
                10L,
                100,
                1L,
                0,
                signature
        ));

        assertThatThrownBy(() -> missionDiscoveryQueryService.getMissions(
                10L,
                MissionCategory.FOOD,
                null,
                null,
                null,
                cursor,
                20
        )).isInstanceOf(MissionException.class);
    }

    @Test
    void rejectsCursorWhenPayloadIsTampered() {
        MissionListCursorCodec codec = new MissionListCursorCodec(CURSOR_SECRET);
        String signature = codec.querySignature(10L, MissionCategory.HISTORY, null, null, null);
        String cursor = codec.encode(new MissionListCursor(
                MissionListCursor.SortMode.RANDOM,
                10L,
                100,
                1L,
                0,
                signature
        ));
        String decoded = new String(
                Base64.getUrlDecoder().decode(cursor),
                StandardCharsets.UTF_8
        );
        String tamperedCursor = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        decoded.replace("|100.0|", "|101.0|")
                                .getBytes(StandardCharsets.UTF_8)
                );

        assertThatThrownBy(() -> codec.decode(tamperedCursor))
                .isInstanceOf(MissionException.class);
    }

    @Test
    void returnsActiveMissionDetailWithDistance() {
        Mission mission = mock(Mission.class);
        when(mission.getId()).thenReturn(1L);
        when(mission.getTitle()).thenReturn("경복궁에서 역사 흔적 찾기");
        when(mission.getDescription()).thenReturn("경복궁에 방문해 주세요.");
        when(mission.getCategory()).thenReturn(MissionCategory.HISTORY);
        when(mission.getSnapshotName()).thenReturn("경복궁");
        when(mission.getSnapshotLatitude()).thenReturn(new BigDecimal("37.5796"));
        when(mission.getSnapshotLongitude()).thenReturn(new BigDecimal("126.9770"));
        when(missionRepository.findByIdAndStatus(1L, MissionStatus.ACTIVE))
                .thenReturn(Optional.of(mission));

        MissionDetailResponseDTO response = missionDiscoveryQueryService.getMission(
                10L,
                1L,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780")
        );

        assertThat(response.distanceMeters()).isPositive();
        assertThat(response.userMissionStatus()).isEqualTo(UserMissionStatus.AVAILABLE);
        assertThat(response.canStart()).isTrue();
        assertThat(response.liked()).isFalse();
        assertThat(response.canCreateArchive()).isFalse();
        verify(missionRepository).findByIdAndStatus(1L, MissionStatus.ACTIVE);
    }

    @Test
    void rejectsLocationWhenOnlyLatitudeIsProvided() {
        assertThatThrownBy(() -> missionDiscoveryQueryService.getMissions(
                10L,
                null,
                null,
                new BigDecimal("37.5665"),
                null,
                null,
                20
        )).isInstanceOf(MissionException.class);
    }

    @Test
    void throwsWhenActiveMissionDoesNotExist() {
        when(missionRepository.findByIdAndStatus(1L, MissionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionDiscoveryQueryService.getMission(10L, 1L, null, null))
                .isInstanceOf(MissionException.class);
    }

    private MissionListProjection projection(Long missionId, MissionCategory category, double sortValue) {
        MissionListProjection projection = mock(MissionListProjection.class);
        when(projection.getMissionId()).thenReturn(missionId);
        when(projection.getTitle()).thenReturn("미션 " + missionId);
        when(projection.getCategory()).thenReturn(category.name());
        when(projection.getSpotName()).thenReturn("장소 " + missionId);
        when(projection.getSortValue()).thenReturn(sortValue);
        return projection;
    }
}
