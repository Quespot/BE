package com.quespot.domain.item.service;

import com.quespot.domain.item.config.ItemMasterDataSeeder;
import com.quespot.domain.item.dto.res.ShopItemResponseDTO;
import com.quespot.domain.item.dto.res.UserItemResponseDTO;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.exception.ItemException;
import com.quespot.domain.item.exception.code.ItemErrorCode;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // 비활성 아이템은 보유 목록·퀘스티·장착에서 빠지되 행은 남는다(#62 spec 2.5). 같은 컨테이너를
    // 다른 테스트와 공유하므로 끝나면 시더를 다시 돌려 되살린다(정의에 있는 아이템이라 재활성화됨).
    @Test
    void inactiveOwnedItemIsHiddenFromListAndCannotBeEquippedButRowIsKept() throws Exception {
        Long userId = 9403L;
        userItemService.getMyItems(userId);
        ShopItem bag = shopItemRepository.findByCode("TRAVEL_BAG").orElseThrow();
        bag.deactivate();
        shopItemRepository.save(bag);

        try {
            assertThat(userItemService.getMyItems(userId)).hasSize(6)
                    .extracting(UserItemResponseDTO::name).doesNotContain("여행 가방");
            assertThat(userItemService.getQuesty(userId).ownedItemCount()).isEqualTo(6);
            assertThatThrownBy(() -> userItemService.equip(userId, bag.getId()))
                    .isInstanceOf(ItemException.class)
                    .extracting(e -> ((ItemException) e).getErrorCode())
                    .isEqualTo(ItemErrorCode.ITEM_NOT_OWNED);
            assertThat(userItemRepository.existsByUserIdAndItem_Id(userId, bag.getId())).isTrue();
        } finally {
            seeder.run();
        }
        assertThat(shopItemRepository.findByCode("TRAVEL_BAG").orElseThrow().getIsActive()).isTrue();
    }

    // 재실행 시 UPDATE가 한 건도 없어야 한다 — updated_at이 그대로인지로 확인한다.
    @Test
    void seederIsIdempotentOnRerunAndSeededBackgroundSlotLimit() throws Exception {
        Map<String, LocalDateTime> before = shopItemRepository.findAll().stream()
                .collect(Collectors.toMap(ShopItem::getCode, ShopItem::getUpdatedAt));

        seeder.run();

        Map<String, LocalDateTime> after = shopItemRepository.findAll().stream()
                .collect(Collectors.toMap(ShopItem::getCode, ShopItem::getUpdatedAt));
        assertThat(after).hasSize(12).isEqualTo(before);
        assertThat(shopItemRepository.findByIsActiveTrue()).hasSize(12);
        assertThat(equipSlotLimitRepository.findById(ItemCategory.BACKGROUND))
                .isPresent()
                .get().extracting(limit -> limit.getMaxEquip()).isEqualTo(1);
    }
}
