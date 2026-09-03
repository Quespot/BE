package com.quespot.domain.item.dto.res;

import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;

public record ShopItemResponseDTO(
        Long id,
        String code,
        String name,
        ItemCategory category,
        ItemRarity rarity,
        Integer price,
        String imageUrl,
        Boolean isFeatured,
        Boolean isDefault
) {
}
