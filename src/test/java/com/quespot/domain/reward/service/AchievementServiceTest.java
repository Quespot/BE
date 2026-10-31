package com.quespot.domain.reward.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.entity.Stamp;
import com.quespot.domain.reward.entity.UserBadge;
import com.quespot.domain.reward.entity.UserStamp;
import com.quespot.domain.reward.enums.ActivityType;
import com.quespot.domain.reward.repository.AchievementMetricRepository;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.RewardActivityRepository;
import com.quespot.domain.reward.repository.StampRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import com.quespot.domain.reward.repository.UserStampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AchievementServiceTest {

    private BadgeRepository badgeRepository;
    private UserBadgeRepository userBadgeRepository;
    private StampRepository stampRepository;
    private UserStampRepository userStampRepository;
    private RewardActivityRepository rewardActivityRepository;
    private AchievementMetricRepository metricRepository;
    private AchievementService service;

    @BeforeEach
    void setUp() {
        badgeRepository = mock(BadgeRepository.class);
        userBadgeRepository = mock(UserBadgeRepository.class);
        stampRepository = mock(StampRepository.class);
        userStampRepository = mock(UserStampRepository.class);
        rewardActivityRepository = mock(RewardActivityRepository.class);
        metricRepository = mock(AchievementMetricRepository.class);
        service = new AchievementService(
                badgeRepository, userBadgeRepository, stampRepository, userStampRepository,
                rewardActivityRepository, metricRepository, new ObjectMapper()
        );
        when(stampRepository.findByRegionCode(anyString())).thenReturn(Optional.empty());
        when(userBadgeRepository.findBadgeIdsByUserId(anyLong())).thenReturn(List.of());
    }

    private Badge badge(Long id, String code, String json) {
        Badge b = Badge.seed(code, code, "설명", json, 1);
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    @Test
    void awardsFirstMissionBadgeWhenThresholdReached() {
        Badge first = badge(1L, "FIRST_MISSION", "{\"metric\":\"MISSION_COMPLETED\",\"threshold\":1}");
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(first));
        when(metricRepository.countCompletedMissions(7L, null)).thenReturn(1L);

        service.onMissionCompleted(7L, "11");

        ArgumentCaptor<UserBadge> saved = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getBadge()).isSameAs(first);

        ArgumentCaptor<RewardActivity> activity = ArgumentCaptor.forClass(RewardActivity.class);
        verify(rewardActivityRepository).save(activity.capture());
        assertThat(activity.getValue().getActivityType()).isEqualTo(ActivityType.BADGE_ACQUIRED);
        assertThat(activity.getValue().getPointTransaction()).isNull();
        assertThat(activity.getValue().getReferenceType()).isEqualTo("BADGE");
        assertThat(activity.getValue().getReferenceId()).isEqualTo(1L);
    }

    @Test
    void doesNotAwardWhenBelowThreshold() {
        Badge seoul = badge(4L, "SEOUL_MASTER",
                "{\"metric\":\"MISSION_COMPLETED\",\"scope\":{\"regionCode\":\"11\"},\"threshold\":10}");
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(seoul));
        when(metricRepository.countCompletedMissions(7L, "11")).thenReturn(9L);

        service.onMissionCompleted(7L, "11");

        verify(userBadgeRepository, never()).save(any());
        verify(rewardActivityRepository, never()).save(any());
    }

    @Test
    void skipsAlreadyAcquiredBadgeWithoutCountingMetric() {
        Badge first = badge(1L, "FIRST_MISSION", "{\"metric\":\"MISSION_COMPLETED\",\"threshold\":1}");
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(first));
        when(userBadgeRepository.findBadgeIdsByUserId(7L)).thenReturn(List.of(1L));

        service.onMissionCompleted(7L, "11");

        verify(metricRepository, never()).countCompletedMissions(anyLong(), any());
        verify(userBadgeRepository, never()).save(any());
    }

    @Test
    void missionTriggerSkipsPhotoMetricAndPhotoTriggerSkipsMissionMetric() {
        Badge photo = badge(3L, "PHOTOGRAPHER", "{\"metric\":\"PHOTO_REGISTERED\",\"threshold\":10}");
        Badge first = badge(1L, "FIRST_MISSION", "{\"metric\":\"MISSION_COMPLETED\",\"threshold\":1}");
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(photo, first));
        when(metricRepository.countCompletedMissions(7L, null)).thenReturn(0L);
        when(metricRepository.countMissionPhotos(7L)).thenReturn(4L);
        when(metricRepository.countArchivePhotos(7L)).thenReturn(6L);

        service.onMissionCompleted(7L, null);
        verify(metricRepository, never()).countMissionPhotos(anyLong());

        service.onPhotoRegistered(7L);
        verify(metricRepository, times(1)).countCompletedMissions(7L, null);   // 첫 호출분만
        ArgumentCaptor<UserBadge> saved = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository).save(saved.capture());
        assertThat(saved.getValue().getBadge()).isSameAs(photo);   // 4 + 6 = 10
    }

    @Test
    void memoizesSameMetricAndScopeWithinOneEvaluation() {
        Badge a = badge(10L, "A", "{\"metric\":\"MISSION_COMPLETED\",\"scope\":{\"regionCode\":\"11\"},\"threshold\":3}");
        Badge b = badge(11L, "B", "{\"metric\":\"MISSION_COMPLETED\",\"scope\":{\"regionCode\":\"11\"},\"threshold\":10}");
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(a, b));
        when(metricRepository.countCompletedMissions(7L, "11")).thenReturn(5L);

        service.onMissionCompleted(7L, "11");

        verify(metricRepository, times(1)).countCompletedMissions(7L, "11");
        verify(userBadgeRepository, times(1)).save(any());
    }

    @Test
    void skipsBadgeWithUnparseableConditionInsteadOfFailing() {
        Badge broken = badge(9L, "BROKEN", "{\"metric\":\"MISSION_COMPLETE_COUNT\",\"scope\":\"ALL\",\"threshold\":1}");
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(broken));

        service.onMissionCompleted(7L, "11");

        verify(userBadgeRepository, never()).save(any());
    }

    @Test
    void awardsStampMatchingRegionOnce() {
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of());
        Stamp seoul = Stamp.seed("SEOUL", "서울", "11", 1, true);
        ReflectionTestUtils.setField(seoul, "id", 1L);
        when(stampRepository.findByRegionCode("11")).thenReturn(Optional.of(seoul));
        when(userStampRepository.existsByUserIdAndStamp_Id(7L, 1L)).thenReturn(false);

        service.onMissionCompleted(7L, "11");

        ArgumentCaptor<UserStamp> saved = ArgumentCaptor.forClass(UserStamp.class);
        verify(userStampRepository).save(saved.capture());
        assertThat(saved.getValue().getStamp()).isSameAs(seoul);
        ArgumentCaptor<RewardActivity> activity = ArgumentCaptor.forClass(RewardActivity.class);
        verify(rewardActivityRepository).save(activity.capture());
        assertThat(activity.getValue().getActivityType()).isEqualTo(ActivityType.STAMP_ACQUIRED);
        assertThat(activity.getValue().getReferenceType()).isEqualTo("STAMP");
    }

    @Test
    void doesNotAwardStampAlreadyAcquiredOrWhenRegionNull() {
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of());
        Stamp seoul = Stamp.seed("SEOUL", "서울", "11", 1, true);
        ReflectionTestUtils.setField(seoul, "id", 1L);
        when(stampRepository.findByRegionCode("11")).thenReturn(Optional.of(seoul));
        when(userStampRepository.existsByUserIdAndStamp_Id(7L, 1L)).thenReturn(true);

        service.onMissionCompleted(7L, "11");
        service.onMissionCompleted(7L, null);

        verify(userStampRepository, never()).save(any());
        verify(stampRepository, never()).findByRegionCode(isNull());
    }
}
