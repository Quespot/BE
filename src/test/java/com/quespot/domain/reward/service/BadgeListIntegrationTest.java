package com.quespot.domain.reward.service;

import com.quespot.domain.reward.dto.res.AchievementSummaryResponseDTO;
import com.quespot.domain.reward.dto.res.BadgeListResponseDTO;
import com.quespot.domain.reward.dto.res.BadgeResponseDTO;
import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.UserBadge;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.UserBadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

// 배지 목록이 비활성 배지를 빼고 내려오는지 실DB로 검증한다(#64).
// 폐기된 웨스트 왕·보류된 부산 탐험은 삭제가 아니라 is_active=false로만 내려가 있어서
// (RewardMasterDataSeeder), 목록이 findAll이면 달성 현황 분모(5)와 어긋난 7개가 나온다.
// 단위 테스트는 리포지토리를 목킹해 이 경로를 증명할 수 없어 통합 테스트로 둔다.
@SpringBootTest
@Testcontainers
class BadgeListIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("quespot_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("app.jwt.secret", () -> "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz");
        registry.add("app.mail.verification-code-secret", () -> "integration-test-mail-secret");
    }

    @Autowired private BadgeService badgeService;
    @Autowired private AchievementQueryService achievementQueryService;
    @Autowired private BadgeRepository badgeRepository;
    @Autowired private UserBadgeRepository userBadgeRepository;

    // 시더가 배지 5개를 활성으로 넣어둔 상태에, 마스터 목록에서 빠진 배지를 실제 운영과
    // 같은 모양(행은 남고 is_active=false)으로 추가한다. 클래스 안 테스트가 컨테이너를
    // 공유하므로 코드 기준으로 한 번만 넣는다.
    @BeforeEach
    void insertDeactivatedBadge() {
        if (badgeRepository.findByCode("BUSAN_EXPLORER").isEmpty()) {
            Badge busan = Badge.seed("BUSAN_EXPLORER", "부산 탐험", "조건 미정으로 보류", "{}", 6);
            busan.deactivate();
            badgeRepository.saveAndFlush(busan);
        }
    }

    @Test
    void excludesDeactivatedBadgeFromList() {
        BadgeListResponseDTO result = badgeService.getBadges(9601L);

        assertThat(result.badges()).extracting(BadgeResponseDTO::code)
                .doesNotContain("BUSAN_EXPLORER")
                .containsExactly("FIRST_MISSION", "EXPLORER", "PHOTOGRAPHER", "SEOUL_MASTER", "QUEST_KING");
    }

    @Test
    void listSizeMatchesAchievementSummaryTotalBadgeCount() {
        Long userId = 9602L;

        BadgeListResponseDTO list = badgeService.getBadges(userId);
        AchievementSummaryResponseDTO summary = achievementQueryService.getSummary(userId);

        assertThat(list.badges()).hasSize((int) summary.totalBadgeCount());
    }

    // 비활성 전에 이미 획득한 사용자가 있어도 목록에서 빠지고, 분자가 분모를 넘지 않는다.
    @Test
    void hidesDeactivatedBadgeEvenWhenUserAlreadyAcquiredIt() {
        Long userId = 9603L;
        Badge busan = badgeRepository.findByCode("BUSAN_EXPLORER").orElseThrow();
        userBadgeRepository.saveAndFlush(UserBadge.acquire(userId, busan));

        BadgeListResponseDTO list = badgeService.getBadges(userId);
        AchievementSummaryResponseDTO summary = achievementQueryService.getSummary(userId);

        assertThat(list.badges()).extracting(BadgeResponseDTO::code).doesNotContain("BUSAN_EXPLORER");
        assertThat(summary.acquiredBadgeCount()).isLessThanOrEqualTo(summary.totalBadgeCount());
    }
}
