package com.quespot.domain.reward.config;

import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.StampRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardMasterDataSeederTest {

    private BadgeRepository badgeRepository;
    private StampRepository stampRepository;
    private RewardMasterDataSeeder seeder;

    @BeforeEach
    void setUp() {
        badgeRepository = mock(BadgeRepository.class);
        stampRepository = mock(StampRepository.class);
        seeder = new RewardMasterDataSeeder(badgeRepository, stampRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedsFiveBadgesAndEightStampsWhenEmpty() throws Exception {
        when(badgeRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(stampRepository.existsByCode(anyString())).thenReturn(false);

        seeder.run();

        ArgumentCaptor<List<Badge>> badges = ArgumentCaptor.forClass(List.class);
        verify(badgeRepository).saveAll(badges.capture());
        assertThat(badges.getValue()).extracting(Badge::getCode)
                .containsExactly("FIRST_MISSION", "EXPLORER", "PHOTOGRAPHER", "SEOUL_MASTER", "QUEST_KING");
        assertThat(badges.getValue()).extracting(Badge::getConditionJson)
                .allMatch(json -> json.contains("\"metric\"") && json.contains("\"threshold\""));
        verify(stampRepository).saveAll(argThat(list -> ((List<?>) list).size() == 8));
    }

    @Test
    void updatesConditionJsonOfExistingBadgeWhenChanged() throws Exception {
        Badge stale = Badge.seed("FIRST_MISSION", "첫 미션", "옛 설명",
                "{\"metric\":\"MISSION_COMPLETE_COUNT\",\"scope\":\"ALL\",\"threshold\":1}", 1);
        when(badgeRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(badgeRepository.findByCode("FIRST_MISSION")).thenReturn(Optional.of(stale));
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(stale));
        when(stampRepository.existsByCode(anyString())).thenReturn(true);

        seeder.run();

        assertThat(stale.getConditionJson()).isEqualTo("{\"metric\":\"MISSION_COMPLETED\",\"threshold\":1}");
        assertThat(stale.getIsActive()).isTrue();
    }

    @Test
    void deactivatesActiveBadgesMissingFromMasterList() throws Exception {
        Badge busan = Badge.seed("BUSAN_EXPLORER", "부산 탐험", "설명", "{}", 5);
        when(badgeRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(busan));
        when(stampRepository.existsByCode(anyString())).thenReturn(true);

        seeder.run();

        assertThat(busan.getIsActive()).isFalse();
    }

    @Test
    void doesNotTouchBadgeWhenDefinitionAlreadyMatches() throws Exception {
        Badge current = Badge.seed("QUEST_KING", "퀘스트 왕", "코스 5개를 완주했어요",
                "{\"metric\":\"COURSE_COMPLETED\",\"threshold\":5}", 5);
        when(badgeRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(badgeRepository.findByCode("QUEST_KING")).thenReturn(Optional.of(current));
        when(badgeRepository.findByIsActiveTrue()).thenReturn(List.of(current));
        when(stampRepository.existsByCode(anyString())).thenReturn(true);

        seeder.run();

        verify(badgeRepository).saveAll(any());  // 나머지 4개 신규 저장
        assertThat(current.getConditionJson()).isEqualTo("{\"metric\":\"COURSE_COMPLETED\",\"threshold\":5}");
        assertThat(current.getIsActive()).isTrue();
    }
}
