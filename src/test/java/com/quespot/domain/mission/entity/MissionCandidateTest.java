package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionCandidateStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MissionCandidateTest {

    @Test
    void generatesCandidateFromCategoryTemplate() {
        Spot spot = spot("경복궁");

        MissionCandidate candidate = MissionCandidate.generate(
                spot,
                MissionTemplate.HISTORY_LOCATION,
                1
        );

        assertThat(candidate.getGeneratedTitle()).isEqualTo("경복궁에서 역사 흔적 찾기");
        assertThat(candidate.getGeneratedDescription()).contains("경복궁");
        assertThat(candidate.getSuggestedCategory()).isEqualTo(MissionCategory.HISTORY);
        assertThat(candidate.getSuggestedRewardPoint()).isEqualTo(100);
        assertThat(candidate.getSuggestedEstimatedMinutes()).isEqualTo(30);
        assertThat(candidate.getStatus()).isEqualTo(MissionCandidateStatus.DRAFT);
    }

    @Test
    void recordsReviewerAndReasonWhenRejected() {
        MissionCandidate candidate = MissionCandidate.generate(
                spot("경복궁"),
                MissionTemplate.HISTORY_LOCATION,
                1
        );
        User reviewer = mock(User.class);

        candidate.reject(reviewer, "이미지가 적절하지 않습니다.");

        assertThat(candidate.getStatus()).isEqualTo(MissionCandidateStatus.REJECTED);
        assertThat(candidate.getReviewedBy()).isSameAs(reviewer);
        assertThat(candidate.getReviewedAt()).isNotNull();
        assertThat(candidate.getReason()).isEqualTo("이미지가 적절하지 않습니다.");
    }

    private Spot spot(String name) {
        Spot spot = mock(Spot.class);
        when(spot.getName()).thenReturn(name);
        return spot;
    }
}
