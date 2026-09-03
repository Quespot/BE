package com.quespot.domain.item.config;

import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.ShopItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

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
    void seedsEquipSlotLimitsAndShopItemsWhenTablesAreEmpty() throws Exception {
        when(equipSlotLimitRepository.existsById(any(ItemCategory.class))).thenReturn(false);
        when(shopItemRepository.existsByCode(anyString())).thenReturn(false);

        seeder.run();

        ArgumentCaptor<List<EquipSlotLimit>> slotLimitsCaptor = ArgumentCaptor.forClass(List.class);
        verify(equipSlotLimitRepository).saveAll(slotLimitsCaptor.capture());
        assertThat(slotLimitsCaptor.getValue()).hasSize(4);

        ArgumentCaptor<List<ShopItem>> shopItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(shopItemRepository).saveAll(shopItemsCaptor.capture());
        assertThat(shopItemsCaptor.getValue()).hasSize(7);
    }

    @Test
    void skipsSeedingWhenAllMasterRowsAlreadyExist() throws Exception {
        when(equipSlotLimitRepository.existsById(any(ItemCategory.class))).thenReturn(true);
        when(shopItemRepository.existsByCode(anyString())).thenReturn(true);

        seeder.run();

        verify(equipSlotLimitRepository, never()).saveAll(any());
        verify(shopItemRepository, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedsOnlyMissingRowsWhenTablesArePartiallyPopulated() throws Exception {
        when(equipSlotLimitRepository.existsById(ItemCategory.HAT)).thenReturn(true);
        when(equipSlotLimitRepository.existsById(ItemCategory.ACCESSORY)).thenReturn(false);
        when(equipSlotLimitRepository.existsById(ItemCategory.OUTFIT)).thenReturn(false);
        when(equipSlotLimitRepository.existsById(ItemCategory.ITEM)).thenReturn(false);

        when(shopItemRepository.existsByCode(anyString())).thenReturn(true);
        when(shopItemRepository.existsByCode("GOLDEN_CROWN")).thenReturn(false);

        seeder.run();

        ArgumentCaptor<List<EquipSlotLimit>> slotLimitsCaptor = ArgumentCaptor.forClass(List.class);
        verify(equipSlotLimitRepository).saveAll(slotLimitsCaptor.capture());
        assertThat(slotLimitsCaptor.getValue())
                .extracting(EquipSlotLimit::getCategory)
                .containsExactly(ItemCategory.ACCESSORY, ItemCategory.OUTFIT, ItemCategory.ITEM);

        ArgumentCaptor<List<ShopItem>> shopItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(shopItemRepository).saveAll(shopItemsCaptor.capture());
        assertThat(shopItemsCaptor.getValue())
                .extracting(ShopItem::getCode)
                .containsExactly("GOLDEN_CROWN");
    }
}
