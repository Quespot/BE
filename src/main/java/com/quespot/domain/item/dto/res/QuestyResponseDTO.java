package com.quespot.domain.item.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.item.enums.ItemRarity;

import java.util.List;

// 홈·마이페이지·꾸미기가 같이 쓴다. highestRarity는 보유 아이템 기준("3개 보유 · 전설 1개"),
// 보유 0이면 null.
public record QuestyResponseDTO(
        @Schema(description = "카테고리 순. 비면 빈 배열이다(null 아님)")
        List<EquippedItemResponseDTO> equippedItems,
        int ownedItemCount,
        @Schema(description = "보유 아이템 중 최고 등급. 보유 0이면 null", nullable = true)
        ItemRarity highestRarity
) {
}
