package com.quespot.domain.mission.service;

import com.quespot.domain.mission.cursor.MissionListCursorCodec;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.MissionSpotQueryRepository;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import com.quespot.domain.mission.repository.projection.MissionSpotSummaryProjection;
import com.quespot.domain.mission.repository.projection.NearbyMissionSpotProjection;
import com.quespot.domain.spot.model.AdministrativeDistrict;
import com.quespot.domain.spot.service.AdministrativeDistrictResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MissionSpotQueryServiceTest {

    private MissionSpotQueryRepository missionSpotQueryRepository;
    private AdministrativeDistrictResolver districtResolver;
    private MissionAttemptStatusResolver statusResolver;
    private MissionSpotQueryService service;

    @BeforeEach
    void setUp() {
        missionSpotQueryRepository = mock(MissionSpotQueryRepository.class);
        districtResolver = mock(AdministrativeDistrictResolver.class);
        statusResolver = mock(MissionAttemptStatusResolver.class);
        service = new MissionSpotQueryService(
                missionSpotQueryRepository,
                districtResolver,
                new MissionListCursorCodec("mission-spot-cursor-secret"),
                statusResolver
        );
    }

    @Test
    void returnsOnlyDistrictsHavingActiveMissionsAndCountsCompletedDistricts() {
        AdministrativeDistrict jongno = district("11110", "종로구");
        AdministrativeDistrict gangnam = district("11680", "강남구");
        MissionSpotSummaryProjection jongnoSummary = summary("11110", 2L, 2L);
        MissionSpotSummaryProjection gangnamSummary = summary("11680", 3L, 1L);
        when(districtResolver.findByRegionCode("11")).thenReturn(List.of(jongno, gangnam));
        when(missionSpotQueryRepository.findMissionSpotSummaries(1L, "11")).thenReturn(List.of(
                jongnoSummary,
                gangnamSummary
        ));

        var response = service.getMissionSpots(1L, "11");

        assertThat(response.totalSpotCount()).isEqualTo(2);
        assertThat(response.completedSpotCount()).isEqualTo(1);
        assertThat(response.totalMissionCount()).isEqualTo(5);
        assertThat(response.completedMissionCount()).isEqualTo(3);
    }

    @Test
    void returnsDistrictMissionPageWithUserStatus() {
        AdministrativeDistrict jongno = district("11110", "종로구");
        MissionListProjection mission = mission(10L, 100.0);
        when(districtResolver.findByCode("11110")).thenReturn(Optional.of(jongno));
        when(missionSpotQueryRepository.findDistrictMissionsRandomly(
                eq("11110"),
                anyLong(),
                isNull(),
                isNull(),
                eq(21)
        )).thenReturn(List.of(mission));
        when(statusResolver.resolveStatuses(eq(1L), any()))
                .thenReturn(Map.of(10L, UserMissionStatus.COMPLETED));

        var response = service.getDistrictMissions(
                1L, "11110", null, null, null, 20
        );

        assertThat(response.missions()).hasSize(1);
        assertThat(response.missions().get(0).userMissionStatus())
                .isEqualTo(UserMissionStatus.COMPLETED);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void excludesNearbyMissionSpotsWithUnsupportedDistrictCodes() {
        AdministrativeDistrict jongno = district("11110", "종로구");
        NearbyMissionSpotProjection supported = nearbySummary("11110", 2L, 1L, 1200.0);
        NearbyMissionSpotProjection unsupported = nearbySummary("99999", 1L, 0L, 1500.0);
        when(missionSpotQueryRepository.findNearbyMissionSpots(
                1L,
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                20
        )).thenReturn(List.of(supported, unsupported));
        when(districtResolver.findByCode("11110")).thenReturn(Optional.of(jongno));
        when(districtResolver.findByCode("99999")).thenReturn(Optional.empty());

        var response = service.getNearbyMissionSpots(
                1L,
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                20
        );

        assertThat(response.missionSpots()).hasSize(1);
        assertThat(response.missionSpots().get(0).districtCode()).isEqualTo("11110");
    }

    private AdministrativeDistrict district(String code, String name) {
        return new AdministrativeDistrict(
                "11",
                "서울특별시",
                code,
                name,
                new BigDecimal("37.5"),
                new BigDecimal("127.0")
        );
    }

    private MissionSpotSummaryProjection summary(
            String code,
            Long missionCount,
            Long completedMissionCount
    ) {
        MissionSpotSummaryProjection projection = mock(MissionSpotSummaryProjection.class);
        when(projection.getDistrictCode()).thenReturn(code);
        when(projection.getMissionCount()).thenReturn(missionCount);
        when(projection.getCompletedMissionCount()).thenReturn(completedMissionCount);
        return projection;
    }

    private MissionListProjection mission(Long missionId, double sortValue) {
        MissionListProjection projection = mock(MissionListProjection.class);
        when(projection.getMissionId()).thenReturn(missionId);
        when(projection.getTitle()).thenReturn("미션");
        when(projection.getCategory()).thenReturn("HISTORY");
        when(projection.getSortValue()).thenReturn(sortValue);
        return projection;
    }

    private NearbyMissionSpotProjection nearbySummary(
            String code,
            Long missionCount,
            Long completedMissionCount,
            Double distanceMeters
    ) {
        NearbyMissionSpotProjection projection = mock(NearbyMissionSpotProjection.class);
        when(projection.getDistrictCode()).thenReturn(code);
        when(projection.getMissionCount()).thenReturn(missionCount);
        when(projection.getCompletedMissionCount()).thenReturn(completedMissionCount);
        when(projection.getDistanceMeters()).thenReturn(distanceMeters);
        return projection;
    }
}
