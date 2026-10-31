package com.quespot.domain.item.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;

import java.time.LocalDateTime;

public record UserItemResponseDTO(
        Long itemId,
        String name,
        ItemCategory category,
        ItemRarity rarity,
        @Schema(description = "없으면 null", nullable = true)
        String imageUrl,
        Boolean isEquipped,
        LocalDateTime purchasedAt,
        @Schema(description = "장착한 적 없으면 null", nullable = true)
        LocalDateTime equippedAt
) {
}
