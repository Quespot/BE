package com.quespot.domain.item.dto.res;

import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;

// 레이어 이미지 URL만 준다 — 서버 합성 없음, 프론트가 겹쳐 그린다.
public record EquippedItemResponseDTO(
        Long itemId,
        ItemCategory category,
        String name,
        String imageUrl,
        ItemRarity rarity
) {
}
