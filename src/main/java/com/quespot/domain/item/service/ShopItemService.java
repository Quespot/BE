package com.quespot.domain.item.service;

import com.quespot.domain.item.converter.ItemConverter;
import com.quespot.domain.item.dto.res.ShopItemResponseDTO;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopItemService {

    private final ShopItemRepository shopItemRepository;

    // 상점엔 판매 아이템만(기본 보유 아이템 제외). 현재 마스터 기준으로는 배경 5종이다(#62).
    @Transactional(readOnly = true)
    public List<ShopItemResponseDTO> getShopItems(ItemCategory category) {
        List<ShopItem> shopItems = category == null
                ? shopItemRepository.findByIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc()
                : shopItemRepository.findByCategoryAndIsActiveTrueAndIsDefaultFalseOrderBySortOrderAsc(category);

        return shopItems.stream()
                .map(ItemConverter::toShopItemResponseDTO)
                .toList();
    }
}
