package com.quespot.domain.item.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;

public record ShopItemResponseDTO(
        Long id,
        String code,
        String name,
        ItemCategory category,
        ItemRarity rarity,
        Integer price,
        @Schema(description = "없으면 null", nullable = true)
        String imageUrl,
        Boolean isFeatured,
        Boolean isDefault
) {
}
