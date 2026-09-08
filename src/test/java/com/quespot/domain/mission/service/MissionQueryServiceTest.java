package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.res.MissionDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionQueryServiceTest {

    private MissionRepository missionRepository;
    private MissionQueryService missionQueryService;

    @BeforeEach
    void setUp() {
        missionRepository = mock(MissionRepository.class);
        missionQueryService = new MissionQueryService(missionRepository, new MissionCursorCodec());
    }

    @Test
    void returnsDistanceOrderedMissionPageWithCursor() {
        MissionListProjection first = projection(1L, MissionCategory.HISTORY, 1200.4);
        MissionListProjection second = projection(2L, MissionCategory.HISTORY, 2300.0);
        when(missionRepository.findMissionListByDistance(
                eq("HISTORY"),
                eq("경복궁"),
                any(BigDecimal.class),
                any(BigDecimal.class),
                isNull(),
                isNull(),
                eq(2)
        )).thenReturn(List.of(first, second));

        MissionListResponseDTO response = missionQueryService.getMissions(
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
    }

    @Test
    void returnsStableRandomOrderWithoutLocation() {
        MissionListProjection mission = projection(1L, MissionCategory.FOOD, 1234.0);
        when(missionRepository.findMissionListRandomly(
                null,
                null,
                anyLong(),
                null,
                null,
                21
        )).thenReturn(List.of(mission));

        MissionListResponseDTO response = missionQueryService.getMissions(
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
        MissionCursorCodec codec = new MissionCursorCodec();
        String signature = codec.querySignature(10L, MissionCategory.HISTORY, null, null, null);
        String cursor = codec.encode(new MissionCursor(
                MissionCursor.SortMode.RANDOM,
                10L,
                100,
                1L,
                signature
        ));

        assertThatThrownBy(() -> missionQueryService.getMissions(
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

        MissionDetailResponseDTO response = missionQueryService.getMission(
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
        assertThatThrownBy(() -> missionQueryService.getMissions(
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

        assertThatThrownBy(() -> missionQueryService.getMission(10L, 1L, null, null))
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
