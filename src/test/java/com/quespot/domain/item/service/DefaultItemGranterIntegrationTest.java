package com.quespot.domain.item.service;

import com.quespot.domain.item.config.ItemMasterDataSeeder;
import com.quespot.domain.item.dto.res.ShopItemResponseDTO;
import com.quespot.domain.item.dto.res.UserItemResponseDTO;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.ShopItemRepository;
import com.quespot.domain.item.repository.UserItemRepository;
import com.quespot.domain.reward.repository.PointTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 기본 아이템 지연 지급의 멱등성, 상점 목록의 기본 아이템 제외, 시더 재실행 멱등성을 실DB로 검증한다(#62).
@SpringBootTest
@Testcontainers
class DefaultItemGranterIntegrationTest {

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

    @Autowired private UserItemService userItemService;
    @Autowired private ShopItemService shopItemService;
    @Autowired private ItemMasterDataSeeder seeder;
    @Autowired private ShopItemRepository shopItemRepository;
    @Autowired private UserItemRepository userItemRepository;
    @Autowired private EquipSlotLimitRepository equipSlotLimitRepository;
    @Autowired private PointTransactionRepository pointTransactionRepository;

    @Test
    void firstListingGrantsSevenDefaultItemsAndRepeatedListingDoesNotDuplicate() {
        Long userId = 9401L;

        List<UserItemResponseDTO> first = userItemService.getMyItems(userId);
        List<UserItemResponseDTO> second = userItemService.getMyItems(userId);

        assertThat(first).hasSize(7);
        assertThat(second).hasSize(7);
        assertThat(first).extracting(UserItemResponseDTO::name).containsExactlyInAnyOrder(
                "탐험가 모자", "보라 선글라스", "빨간 스카프", "노란 우비", "탐험 조끼", "여행 가방", "탐험 돋보기");
        assertThat(first).allMatch(item -> !item.isEquipped());
        assertThat(userItemRepository.findAll()).filteredOn(ui -> ui.getUserId().equals(userId)).hasSize(7);
        assertThat(pointTransactionRepository.findAll()).filteredOn(t -> t.getUserId().equals(userId)).isEmpty();
    }

    @Test
    void equipGrantsDefaultItemsBeforeLookingUpOwnership() {
        Long userId = 9402L;
        ShopItem hat = shopItemRepository.findByCode("EXPLORER_HAT").orElseThrow();

        userItemService.equip(userId, hat.getId());

        assertThat(userItemRepository.findByUserIdAndItem_Id(userId, hat.getId()))
                .isPresent()
                .get().extracting(ui -> ui.getIsEquipped()).isEqualTo(true);
        assertThat(userItemService.getQuesty(userId).ownedItemCount()).isEqualTo(7);
    }

    @Test
    void shopListsOnlyTheFiveBackgrounds() {
        List<ShopItemResponseDTO> all = shopItemService.getShopItems(null);
        List<ShopItemResponseDTO> backgrounds = shopItemService.getShopItems(ItemCategory.BACKGROUND);
        List<ShopItemResponseDTO> hats = shopItemService.getShopItems(ItemCategory.HAT);

        assertThat(all).hasSize(5).allMatch(item -> item.category() == ItemCategory.BACKGROUND && !item.isDefault());
        assertThat(all).extracting(ShopItemResponseDTO::code).containsExactly(
                "COZY_CAFE", "SUNNY_CLASSROOM", "COZY_ROOM", "BEACH_CAMPING", "STARLIGHT_OBSERVATORY");
        assertThat(backgrounds).hasSize(5);
        assertThat(hats).isEmpty();
    }

    @Test
    void seederIsIdempotentOnRerunAndSeededBackgroundSlotLimit() throws Exception {
        long before = shopItemRepository.count();

        seeder.run();

        assertThat(shopItemRepository.count()).isEqualTo(before).isEqualTo(12);
        assertThat(shopItemRepository.findByIsActiveTrue()).hasSize(12);
        assertThat(equipSlotLimitRepository.findById(ItemCategory.BACKGROUND))
                .isPresent()
                .get().extracting(limit -> limit.getMaxEquip()).isEqualTo(1);
    }
}
