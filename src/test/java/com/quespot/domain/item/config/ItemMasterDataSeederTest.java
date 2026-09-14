package com.quespot.domain.item.config;

import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.ShopItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemMasterDataSeederTest {

    private EquipSlotLimitRepository equipSlotLimitRepository;
    private ShopItemRepository shopItemRepository;
    private ItemMasterDataSeeder seeder;

    @BeforeEach
    void setUp() {
        equipSlotLimitRepository = mock(EquipSlotLimitRepository.class);
        shopItemRepository = mock(ShopItemRepository.class);
        seeder = new ItemMasterDataSeeder(equipSlotLimitRepository, shopItemRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedsFiveSlotLimitsAndTwelveShopItemsWhenTablesAreEmpty() throws Exception {
        when(equipSlotLimitRepository.existsById(any(ItemCategory.class))).thenReturn(false);
        when(shopItemRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(shopItemRepository.findByIsActiveTrue()).thenReturn(List.of());

        seeder.run();

        ArgumentCaptor<List<EquipSlotLimit>> slotLimitsCaptor = ArgumentCaptor.forClass(List.class);
        verify(equipSlotLimitRepository).saveAll(slotLimitsCaptor.capture());
        assertThat(slotLimitsCaptor.getValue())
                .extracting(EquipSlotLimit::getCategory)
                .containsExactly(ItemCategory.HAT, ItemCategory.ACCESSORY, ItemCategory.OUTFIT,
                        ItemCategory.ITEM, ItemCategory.BACKGROUND);

        ArgumentCaptor<List<ShopItem>> shopItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(shopItemRepository).saveAll(shopItemsCaptor.capture());
        List<ShopItem> saved = shopItemsCaptor.getValue();
        assertThat(saved).hasSize(12);
        assertThat(saved).filteredOn(ShopItem::getIsDefault).hasSize(7)
                .allSatisfy(item -> {
                    assertThat(item.getPrice()).isZero();
                    assertThat(item.getCategory()).isNotEqualTo(ItemCategory.BACKGROUND);
                });
        assertThat(saved).filteredOn(item -> !item.getIsDefault()).hasSize(5)
                .allSatisfy(item -> {
                    assertThat(item.getCategory()).isEqualTo(ItemCategory.BACKGROUND);
                    assertThat(item.getPrice()).isPositive();
                });
        assertThat(saved).extracting(ShopItem::getCode).contains(
                "EXPLORER_HAT", "PURPLE_SUNGLASSES", "RED_SCARF", "YELLOW_RAINCOAT",
                "EXPLORER_VEST", "TRAVEL_BAG", "EXPLORER_MAGNIFIER",
                "COZY_CAFE", "SUNNY_CLASSROOM", "COZY_ROOM", "BEACH_CAMPING", "STARLIGHT_OBSERVATORY");
        assertThat(saved).filteredOn(item -> "STARLIGHT_OBSERVATORY".equals(item.getCode()))
                .singleElement().extracting(ShopItem::getPrice).isEqualTo(280);
    }

    @Test
    void doesNothingWhenEveryRowAlreadyMatchesDefinition() throws Exception {
        when(equipSlotLimitRepository.existsById(any(ItemCategory.class))).thenReturn(true);
        List<ShopItem> existing = ItemMasterDataSeeder.SHOP_ITEMS.stream()
                .map(ItemMasterDataSeeder.ShopItemDefinition::toEntity)
                .toList();
        for (ShopItem item : existing) {
            when(shopItemRepository.findByCode(item.getCode())).thenReturn(Optional.of(item));
        }
        when(shopItemRepository.findByIsActiveTrue()).thenReturn(existing);

        seeder.run();

        verify(equipSlotLimitRepository, never()).saveAll(any());
        verify(shopItemRepository, never()).saveAll(any());
        assertThat(existing).allMatch(ShopItem::getIsActive);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatesExistingRowWhoseDefinitionChangedAndInsertsTheRest() throws Exception {
        when(equipSlotLimitRepository.existsById(any(ItemCategory.class))).thenReturn(true);
        ShopItem staleHat = ShopItem.seed("EXPLORER_HAT", "옛 이름", ItemCategory.HAT, ItemRarity.RARE, 50, false, true, 99);
        when(shopItemRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(shopItemRepository.findByCode("EXPLORER_HAT")).thenReturn(Optional.of(staleHat));
        when(shopItemRepository.findByIsActiveTrue()).thenReturn(List.of(staleHat));

        seeder.run();

        assertThat(staleHat.getName()).isEqualTo("탐험가 모자");
        assertThat(staleHat.getRarity()).isEqualTo(ItemRarity.NORMAL);
        assertThat(staleHat.getPrice()).isZero();
        assertThat(staleHat.getIsDefault()).isTrue();
        assertThat(staleHat.getIsFeatured()).isFalse();
        assertThat(staleHat.getSortOrder()).isEqualTo(1);
        assertThat(staleHat.getIsActive()).isTrue();

        ArgumentCaptor<List<ShopItem>> shopItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(shopItemRepository).saveAll(shopItemsCaptor.capture());
        assertThat(shopItemsCaptor.getValue()).hasSize(11)
                .extracting(ShopItem::getCode).doesNotContain("EXPLORER_HAT");
    }

    @Test
    void deactivatesActiveItemsMissingFromDefinitionsAndReactivatesKnownOnes() throws Exception {
        when(equipSlotLimitRepository.existsById(any(ItemCategory.class))).thenReturn(true);
        ShopItem legacyCrown = ShopItem.seed("GOLDEN_CROWN", "황금 왕관", ItemCategory.HAT, ItemRarity.LEGENDARY, 300, false, false, 0);
        ShopItem legacyScarf = ShopItem.seed("BLUE_SCARF", "파란 스카프", ItemCategory.ACCESSORY, ItemRarity.NORMAL, 0, true, false, 0);
        ShopItem inactiveBag = ShopItem.seed("TRAVEL_BAG", "여행 가방", ItemCategory.ITEM, ItemRarity.NORMAL, 0, true, false, 6);
        inactiveBag.deactivate();
        when(shopItemRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(shopItemRepository.findByCode("TRAVEL_BAG")).thenReturn(Optional.of(inactiveBag));
        when(shopItemRepository.findByIsActiveTrue()).thenReturn(List.of(legacyCrown, legacyScarf));

        seeder.run();

        assertThat(legacyCrown.getIsActive()).isFalse();
        assertThat(legacyScarf.getIsActive()).isFalse();
        assertThat(inactiveBag.getIsActive()).isTrue();
    }
}
