package com.quespot.domain.item.config;

import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.ShopItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(equipSlotLimitRepository.count()).thenReturn(0L);
        when(shopItemRepository.count()).thenReturn(0L);

        seeder.run();

        ArgumentCaptor<List<EquipSlotLimit>> slotLimitsCaptor = ArgumentCaptor.forClass(List.class);
        verify(equipSlotLimitRepository).saveAll(slotLimitsCaptor.capture());
        assertThat(slotLimitsCaptor.getValue()).hasSize(4);

        ArgumentCaptor<List<ShopItem>> shopItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(shopItemRepository).saveAll(shopItemsCaptor.capture());
        assertThat(shopItemsCaptor.getValue()).hasSize(7);
    }

    @Test
    void skipsSeedingWhenTablesAlreadyHaveData() throws Exception {
        when(equipSlotLimitRepository.count()).thenReturn(4L);
        when(shopItemRepository.count()).thenReturn(7L);

        seeder.run();

        verify(equipSlotLimitRepository, never()).saveAll(any());
        verify(shopItemRepository, never()).saveAll(any());
    }
}
