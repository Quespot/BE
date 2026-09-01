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

    @Transactional(readOnly = true)
    public List<ShopItemResponseDTO> getShopItems(ItemCategory category) {
        List<ShopItem> shopItems = category == null
                ? shopItemRepository.findByIsActiveTrueOrderBySortOrderAsc()
                : shopItemRepository.findByCategoryAndIsActiveTrueOrderBySortOrderAsc(category);

        return shopItems.stream()
                .map(ItemConverter::toShopItemResponseDTO)
                .toList();
    }
}
