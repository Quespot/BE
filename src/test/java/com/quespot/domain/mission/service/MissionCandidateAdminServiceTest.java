package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.res.MissionCandidateBatchResponseDTO;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionCandidateStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MissionCandidateAdminServiceTest {

    private MissionCandidateRepository missionCandidateRepository;
    private MissionRepository missionRepository;
    private UserRepository userRepository;
    private MissionCandidateAdminService service;

    @BeforeEach
    void setUp() {
        missionCandidateRepository = mock(MissionCandidateRepository.class);
        missionRepository = mock(MissionRepository.class);
        userRepository = mock(UserRepository.class);
        service = new MissionCandidateAdminService(
                missionCandidateRepository,
                missionRepository,
                userRepository
        );
    }

    @Test
    void approvesAllDraftCandidatesInSelectedCategory() {
        MissionCandidate candidate = candidate();
        User reviewer = admin();
        when(missionCandidateRepository.findAllByStatusAndCategoryForUpdate(
                MissionCandidateStatus.DRAFT,
                MissionCategory.HISTORY
        )).thenReturn(List.of(candidate));
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));

        MissionCandidateBatchResponseDTO result = service.approveAll(MissionCategory.HISTORY, 10L);

        assertThat(candidate.getStatus()).isEqualTo(MissionCandidateStatus.APPROVED);
        assertThat(candidate.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.processedCount()).isEqualTo(1);
    }

    @Test
    void publishesAllApprovedCandidatesInSelectedCategory() {
        MissionCandidate candidate = candidate();
        candidate.approve(admin());
        when(missionCandidateRepository.findAllByStatusAndCategoryWithSpotForUpdate(
                MissionCandidateStatus.APPROVED,
                MissionCategory.HISTORY
        )).thenReturn(List.of(candidate));
        when(missionRepository.findPublishedCandidateIds(List.of(1L))).thenReturn(List.of());

        MissionCandidateBatchResponseDTO result = service.publishAll(MissionCategory.HISTORY);

        assertThat(candidate.getStatus()).isEqualTo(MissionCandidateStatus.PUBLISHED);
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
    }

    @Test
    void returnsEmptyResultWhenCategoryHasNoApprovedCandidates() {
        when(missionCandidateRepository.findAllByStatusAndCategoryWithSpotForUpdate(
                MissionCandidateStatus.APPROVED,
                MissionCategory.FOOD
        )).thenReturn(List.of());

        MissionCandidateBatchResponseDTO result = service.publishAll(MissionCategory.FOOD);

        assertThat(result.targetCount()).isZero();
        assertThat(result.processedCount()).isZero();
    }

    private MissionCandidate candidate() {
        Spot spot = mock(Spot.class);
        when(spot.getName()).thenReturn("경복궁");
        when(spot.getLatitude()).thenReturn(new BigDecimal("37.579617"));
        when(spot.getLongitude()).thenReturn(new BigDecimal("126.977041"));
        MissionCandidate candidate = MissionCandidate.generate(spot, MissionTemplate.HISTORY_LOCATION, 1);
        ReflectionTestUtils.setField(candidate, "id", 1L);
        return candidate;
    }

    private User admin() {
        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.ADMIN);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        return user;
    }
}
