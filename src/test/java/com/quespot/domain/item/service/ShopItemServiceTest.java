package com.quespot.domain.item.service;

import com.quespot.domain.item.dto.res.ShopItemResponseDTO;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;
import com.quespot.domain.item.repository.ShopItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopItemServiceTest {

    private ShopItemRepository shopItemRepository;
    private ShopItemService shopItemService;

    @BeforeEach
    void setUp() {
        shopItemRepository = mock(ShopItemRepository.class);
        shopItemService = new ShopItemService(shopItemRepository);
    }

    @Test
    void listsAllActiveItemsWhenCategoryIsNull() {
        ShopItem item = ShopItem.seed("EXPLORER_HAT", "탐험가 모자", ItemCategory.HAT, ItemRarity.NORMAL, 0, true, false);
        when(shopItemRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(item));

        List<ShopItemResponseDTO> result = shopItemService.getShopItems(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("EXPLORER_HAT");
        verify(shopItemRepository, never()).findByCategoryAndIsActiveTrueOrderBySortOrderAsc(any());
    }

    @Test
    void listsItemsByCategoryWhenCategoryProvided() {
        ShopItem item = ShopItem.seed("GOLDEN_CROWN", "황금 왕관", ItemCategory.HAT, ItemRarity.LEGENDARY, 300, false, false);
        when(shopItemRepository.findByCategoryAndIsActiveTrueOrderBySortOrderAsc(ItemCategory.HAT))
                .thenReturn(List.of(item));

        List<ShopItemResponseDTO> result = shopItemService.getShopItems(ItemCategory.HAT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rarity()).isEqualTo(ItemRarity.LEGENDARY);
        verify(shopItemRepository, never()).findByIsActiveTrueOrderBySortOrderAsc();
    }
}
