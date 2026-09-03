package com.quespot.domain.item.converter;

import com.quespot.domain.item.dto.res.ShopItemResponseDTO;
import com.quespot.domain.item.dto.res.UserItemResponseDTO;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.entity.UserItem;

public class ItemConverter {

    private ItemConverter() {
    }

    public static ShopItemResponseDTO toShopItemResponseDTO(ShopItem shopItem) {
        return new ShopItemResponseDTO(
                shopItem.getId(),
                shopItem.getCode(),
                shopItem.getName(),
                shopItem.getCategory(),
                shopItem.getRarity(),
                shopItem.getPrice(),
                shopItem.getImageUrl(),
                shopItem.getIsFeatured(),
                shopItem.getIsDefault()
        );
    }

    public static UserItemResponseDTO toUserItemResponseDTO(UserItem userItem) {
        ShopItem item = userItem.getItem();

        return new UserItemResponseDTO(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getRarity(),
                item.getImageUrl(),
                userItem.getIsEquipped(),
                userItem.getPurchasedAt(),
                userItem.getEquippedAt()
        );
    }
}
