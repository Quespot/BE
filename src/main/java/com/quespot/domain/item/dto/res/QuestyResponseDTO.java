package com.quespot.domain.item.dto.res;

import com.quespot.domain.item.enums.ItemRarity;

import java.util.List;

// 홈·마이페이지·꾸미기가 같이 쓴다. highestRarity는 보유 아이템 기준("3개 보유 · 전설 1개"),
// 보유 0이면 null.
public record QuestyResponseDTO(
        List<EquippedItemResponseDTO> equippedItems,
        int ownedItemCount,
        ItemRarity highestRarity
) {
}
