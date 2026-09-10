package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionSource;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.spot.entity.Spot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MissionTest {

    @Test
    void publishesMissionWithSpotSnapshot() {
        Spot spot = mock(Spot.class);
        when(spot.getName()).thenReturn("경복궁");
        when(spot.getAddress()).thenReturn("서울 종로구 사직로 161");
        when(spot.getLatitude()).thenReturn(new BigDecimal("37.579617"));
        when(spot.getLongitude()).thenReturn(new BigDecimal("126.977041"));
        when(spot.getImageUrl()).thenReturn("https://example.com/gyeongbokgung.jpg");
        when(spot.getDistrictCode()).thenReturn("11110");
        MissionCandidate candidate = MissionCandidate.generate(spot, MissionTemplate.HISTORY_LOCATION, 1);

        Mission mission = Mission.publish(candidate);

        assertThat(mission.getSpot()).isSameAs(spot);
        assertThat(mission.getCandidate()).isSameAs(candidate);
        assertThat(mission.getSnapshotName()).isEqualTo("경복궁");
        assertThat(mission.getSnapshotAddress()).isEqualTo("서울 종로구 사직로 161");
        assertThat(mission.getSnapshotLatitude()).isEqualByComparingTo("37.579617");
        assertThat(mission.getSnapshotLongitude()).isEqualByComparingTo("126.977041");
        assertThat(mission.getSnapshotImageUrl()).isEqualTo("https://example.com/gyeongbokgung.jpg");
        assertThat(mission.getSnapshotDistrictCode()).isEqualTo("11110");
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.ACTIVE);
        assertThat(mission.getSource()).isEqualTo(MissionSource.TOUR_API);
    }
}
