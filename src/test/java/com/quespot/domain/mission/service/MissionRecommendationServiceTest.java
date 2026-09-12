package com.quespot.domain.mission.service;

import com.quespot.domain.like.enums.LikeTargetType;
import com.quespot.domain.like.repository.LikeRepository;
import com.quespot.domain.mission.cursor.MissionRecommendationCursorCodec;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.repository.MissionRecommendationQueryRepository;
import com.quespot.domain.mission.repository.projection.RecommendedMissionProjection;
import com.quespot.domain.user.entity.UserProfile;
import com.quespot.domain.user.enums.TravelStyle;
import com.quespot.domain.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionRecommendationServiceTest {

    private MissionRecommendationQueryRepository missionRecommendationQueryRepository;
    private UserProfileRepository userProfileRepository;
    private MissionAttemptStatusResolver missionAttemptStatusResolver;
    private LikeRepository likeRepository;
    private MissionRecommendationService service;

    @BeforeEach
    void setUp() {
        missionRecommendationQueryRepository = mock(MissionRecommendationQueryRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        missionAttemptStatusResolver = mock(MissionAttemptStatusResolver.class);
        likeRepository = mock(LikeRepository.class);
        service = new MissionRecommendationService(
                missionRecommendationQueryRepository,
                userProfileRepository,
                missionAttemptStatusResolver,
                likeRepository,
                new MissionRecommendationCursorCodec("recommendation-service-test-secret")
        );
        when(missionAttemptStatusResolver.resolveStatuses(anyLong(), any())).thenReturn(Map.of());
    }

    @Test
    void recommendsPreferredAvailableMissionsWithDistanceAndLikedStatus() {
        UserProfile profile = mock(UserProfile.class);
        when(profile.getTravelStyles()).thenReturn(Set.of(TravelStyle.NATURE, TravelStyle.FOOD));
        when(userProfileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        BigDecimal latitude = new BigDecimal("37.5665");
        BigDecimal longitude = new BigDecimal("126.9780");
        RecommendedMissionProjection food = projection(1L, MissionCategory.FOOD, 0, 1, 10, 1200.4);
        RecommendedMissionProjection nature = projection(2L, MissionCategory.NATURE, 0, 1, 20, 1800.0);
        RecommendedMissionProjection fallback = projection(3L, MissionCategory.HISTORY, 1, 1, 30, 2300.0);
        when(missionRecommendationQueryRepository.findRecommendedMissions(
                eq(10L),
                eq("FOOD,NATURE"),
                eq(latitude),
                eq(longitude),
                anyLong(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(30)
        )).thenReturn(List.of(food, nature, fallback));
        when(likeRepository.findLikedTargetIds(
                10L, LikeTargetType.MISSION, List.of(1L, 2L)
        )).thenReturn(List.of(2L));

        var result = service.getRecommendations(10L, latitude, longitude, null, 2);

        assertThat(result.missions()).extracting("missionId").containsExactly(1L, 2L);
        assertThat(result.missions()).extracting("distanceMeters").containsExactly(1200L, 1800L);
        assertThat(result.missions()).extracting("liked").containsExactly(false, true);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotBlank();
    }

    @Test
    void excludesLockedMissionAndFallsBackWithoutProfileOrLocation() {
        when(userProfileRepository.findByUserId(10L)).thenReturn(Optional.empty());
        RecommendedMissionProjection locked = projection(1L, MissionCategory.FOOD, 0, 1, 10, 100.0);
        RecommendedMissionProjection available = projection(2L, MissionCategory.HISTORY, 0, 1, 20, 200.0);
        when(missionRecommendationQueryRepository.findRecommendedMissions(
                eq(10L),
                eq(""),
                isNull(),
                isNull(),
                anyLong(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(30)
        )).thenReturn(List.of(locked, available));
        when(missionAttemptStatusResolver.resolveStatuses(10L, List.of(1L, 2L)))
                .thenReturn(Map.of(1L, UserMissionStatus.LOCKED));
        when(likeRepository.findLikedTargetIds(10L, LikeTargetType.MISSION, List.of(2L)))
                .thenReturn(List.of());

        var result = service.getRecommendations(10L, null, null, null, 2);

        assertThat(result.missions()).extracting("missionId").containsExactly(2L);
        assertThat(result.missions().get(0).distanceMeters()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void returnsEmptyResultWithoutQueryingLikesWhenNoCandidateExists() {
        when(userProfileRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(missionRecommendationQueryRepository.findRecommendedMissions(
                eq(10L), eq(""), isNull(), isNull(), anyLong(),
                isNull(), isNull(), isNull(), isNull(), isNull(), anyInt()
        )).thenReturn(List.of());

        var result = service.getRecommendations(10L, null, null, null, 20);

        assertThat(result.missions()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        verify(likeRepository, never()).findLikedTargetIds(anyLong(), any(), any());
    }

    @Test
    void rejectsPartialLocation() {
        assertThatThrownBy(() -> service.getRecommendations(
                10L, new BigDecimal("37.5"), null, null, 2
        )).isInstanceOf(MissionException.class);
    }

    private RecommendedMissionProjection projection(
            Long missionId,
            MissionCategory category,
            int preferenceRank,
            long categoryRank,
            long categoryOrder,
            double sortValue
    ) {
        RecommendedMissionProjection projection = mock(RecommendedMissionProjection.class);
        when(projection.getMissionId()).thenReturn(missionId);
        when(projection.getTitle()).thenReturn("추천 미션 " + missionId);
        when(projection.getCategory()).thenReturn(category.name());
        when(projection.getSpotName()).thenReturn("추천 장소 " + missionId);
        when(projection.getRewardPoint()).thenReturn(100);
        when(projection.getEstimatedMinutes()).thenReturn(30);
        when(projection.getPreferenceRank()).thenReturn(preferenceRank);
        when(projection.getCategoryRank()).thenReturn(categoryRank);
        when(projection.getCategoryOrder()).thenReturn(categoryOrder);
        when(projection.getSortValue()).thenReturn(sortValue);
        return projection;
    }
}
