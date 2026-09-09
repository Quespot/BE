package com.quespot.domain.mission.service;

import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MissionAttemptStatusResolverTest {

    private MissionAttemptRepository missionAttemptRepository;
    private MissionAttemptStatusResolver resolver;

    @BeforeEach
    void setUp() {
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        resolver = new MissionAttemptStatusResolver(missionAttemptRepository);
    }

    private MissionAttemptStatusProjection projectionOf(Long missionId, MissionAttemptStatus status) {
        MissionAttemptStatusProjection projection = mock(MissionAttemptStatusProjection.class);
        when(projection.getMissionId()).thenReturn(missionId);
        when(projection.getStatus()).thenReturn(status);
        return projection;
    }

    @Test
    void resolveStatusesReturnsCompletedOverInProgressAndDefaultsToAvailable() {
        MissionAttemptStatusProjection completed = projectionOf(1L, MissionAttemptStatus.COMPLETED);
        MissionAttemptStatusProjection inProgress = projectionOf(2L, MissionAttemptStatus.IN_PROGRESS);
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(
                anyLong(), any(), any()
        )).thenReturn(List.of(completed, inProgress));

        Map<Long, UserMissionStatus> statuses = resolver.resolveStatuses(1L, List.of(1L, 2L, 3L));

        assertThat(statuses.get(1L)).isEqualTo(UserMissionStatus.COMPLETED);
        assertThat(statuses.get(2L)).isEqualTo(UserMissionStatus.IN_PROGRESS);
        assertThat(statuses.getOrDefault(3L, UserMissionStatus.AVAILABLE)).isEqualTo(UserMissionStatus.AVAILABLE);
    }

    @Test
    void resolveStatusCallsBulkMethodWithSingleElementList() {
        MissionAttemptStatusProjection inProgress = projectionOf(5L, MissionAttemptStatus.IN_PROGRESS);
        when(missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(
                anyLong(), any(), any()
        )).thenReturn(List.of(inProgress));

        UserMissionStatus status = resolver.resolveStatus(1L, 5L);

        assertThat(status).isEqualTo(UserMissionStatus.IN_PROGRESS);
    }
}
