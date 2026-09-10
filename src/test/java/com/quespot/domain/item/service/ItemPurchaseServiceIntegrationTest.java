package com.quespot.domain.item.service;

import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.exception.ItemException;
import com.quespot.domain.item.exception.code.ItemErrorCode;
import com.quespot.domain.item.repository.ShopItemRepository;
import com.quespot.domain.item.repository.UserItemRepository;
import com.quespot.domain.reward.exception.RewardException;
import com.quespot.domain.reward.repository.PointTransactionRepository;
import com.quespot.domain.reward.repository.UserPointRepository;
import com.quespot.domain.reward.service.PointService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 조건부 차감 + UNIQUE(user_id, item_id) + 롤백을 실DB로 검증한다.
// 시더(ItemMasterDataSeeder)가 부팅 시 GOLDEN_CROWN(300P) 등을 넣어둔다.
@SpringBootTest
@Testcontainers
class ItemPurchaseServiceIntegrationTest {

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

    @Autowired private ItemPurchaseService itemPurchaseService;
    @Autowired private ShopItemRepository shopItemRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private UserPointRepository userPointRepository;
    @Autowired private PointTransactionRepository pointTransactionRepository;
    @Autowired private PointService pointService;

    private ShopItem goldenCrown() {
        return shopItemRepository.findAll().stream()
                .filter(i -> "GOLDEN_CROWN".equals(i.getCode()))
                .findFirst().orElseThrow();
    }

    @Test
    void purchaseDebitsAndGrantsThenSecondPurchaseIsRejectedWithoutSecondDebit() {
        Long userId = 9301L;
        ShopItem crown = goldenCrown();
        pointService.credit(userId, 500, "MISSION_REWARD", "MISSION_ATTEMPT", 9301L, "보상");

        itemPurchaseService.purchase(userId, crown.getId());

        assertThat(userItemRepository.existsByUserIdAndItem_Id(userId, crown.getId())).isTrue();
        assertThat(userPointRepository.findById(userId).get().getBalance()).isEqualTo(500 - crown.getPrice());

        assertThatThrownBy(() -> itemPurchaseService.purchase(userId, crown.getId()))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode())
                .isEqualTo(ItemErrorCode.ITEM_ALREADY_OWNED);
        assertThat(userPointRepository.findById(userId).get().getBalance()).isEqualTo(500 - crown.getPrice());
    }

    @Test
    void insufficientBalanceRejectsPurchaseWithoutGrantingItem() {
        Long userId = 9302L;
        ShopItem crown = goldenCrown();
        pointService.credit(userId, 100, "MISSION_REWARD", "MISSION_ATTEMPT", 9302L, "보상");

        assertThatThrownBy(() -> itemPurchaseService.purchase(userId, crown.getId()))
                .isInstanceOf(RewardException.class);

        assertThat(userItemRepository.existsByUserIdAndItem_Id(userId, crown.getId())).isFalse();
        assertThat(userPointRepository.findById(userId).get().getBalance()).isEqualTo(100);
        assertThat(pointTransactionRepository.findAll())
                .filteredOn(t -> t.getUserId().equals(userId))
                .hasSize(1);   // credit만
    }
}
