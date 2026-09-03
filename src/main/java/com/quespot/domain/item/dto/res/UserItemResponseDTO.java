package com.quespot.domain.item.dto.res;

import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;

import java.time.LocalDateTime;

public record UserItemResponseDTO(
        Long itemId,
        String name,
        ItemCategory category,
        ItemRarity rarity,
        String imageUrl,
        Boolean isEquipped,
        LocalDateTime purchasedAt,
        LocalDateTime equippedAt
) {
}
