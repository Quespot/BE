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
    void listsActiveNonDefaultItemsWhenCategoryIsNull() {
        ShopItem item = ShopItem.seed("COZY_CAFE", "포근한 카페", ItemCategory.BACKGROUND, ItemRarity.NORMAL, 320, false, false, 11);
        when(shopItemRepository.findByIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc()).thenReturn(List.of(item));

        List<ShopItemResponseDTO> result = shopItemService.getShopItems(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("COZY_CAFE");
        assertThat(result.get(0).isDefault()).isFalse();
        verify(shopItemRepository, never()).findByCategoryAndIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc(any());
    }

    @Test
    void listsActiveNonDefaultItemsByCategoryWhenCategoryProvided() {
        ShopItem item = ShopItem.seed("BEACH_CAMPING", "바닷가 캠핑", ItemCategory.BACKGROUND, ItemRarity.NORMAL, 360, false, false, 14);
        when(shopItemRepository.findByCategoryAndIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc(ItemCategory.BACKGROUND))
                .thenReturn(List.of(item));

        List<ShopItemResponseDTO> result = shopItemService.getShopItems(ItemCategory.BACKGROUND);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).price()).isEqualTo(360);
        verify(shopItemRepository, never()).findByIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc();
    }
}
