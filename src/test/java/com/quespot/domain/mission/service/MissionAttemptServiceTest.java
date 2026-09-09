package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionAttemptServiceTest {

    private MissionAttemptRepository missionAttemptRepository;
    private MissionRepository missionRepository;
    private MissionAttemptService missionAttemptService;

    @BeforeEach
    void setUp() {
        missionAttemptRepository = mock(MissionAttemptRepository.class);
        missionRepository = mock(MissionRepository.class);
        missionAttemptService = new MissionAttemptService(missionAttemptRepository, missionRepository);
    }

    private Mission activeMission() {
        Spot spot = Spot.builder()
                .source(SpotSource.TOUR_API).sourceContentId("1").name("테스트 스팟")
                .latitude(new BigDecimal("37.5665")).longitude(new BigDecimal("126.9780"))
                .appCategory(AppCategory.CULTURE).categoryMappingVersion(1).showFlag(true)
                .build();
        MissionCandidate candidate = MissionCandidate.generate(spot, MissionTemplate.CULTURE_LOCATION, 1);
        return Mission.publish(candidate);
    }

    @Test
    void startCreatesNewAttemptWhenNoneExists() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(missionAttemptRepository.save(any(MissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MissionAttempt attempt = missionAttemptService.start(1L, 10L);

        assertThat(attempt.getStatus()).isEqualTo(MissionAttemptStatus.IN_PROGRESS);
        verify(missionAttemptRepository).save(any(MissionAttempt.class));
    }

    @Test
    void startReturnsExistingAttemptWhenAlreadyInProgress() {
        Mission mission = activeMission();
        MissionAttempt existing = MissionAttempt.start(1L, mission);
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existing));

        MissionAttempt attempt = missionAttemptService.start(1L, 10L);

        assertThat(attempt).isSameAs(existing);
        verify(missionAttemptRepository, never()).save(any(MissionAttempt.class));
    }

    @Test
    void startThrowsWhenAlreadyCompleted() {
        Mission mission = activeMission();
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.of(mission));
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(missionAttemptRepository.findByUserIdAndMissionIdAndStatus(1L, 10L, MissionAttemptStatus.COMPLETED))
                .thenReturn(Optional.of(MissionAttempt.start(1L, mission)));

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_ALREADY_COMPLETED);
    }

    @Test
    void startThrowsWhenMissionNotActive() {
        when(missionRepository.findByIdAndStatus(10L, MissionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionAttemptService.start(1L, 10L))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.MISSION_NOT_FOUND);
    }
}
