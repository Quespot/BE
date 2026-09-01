package com.quespot.domain.item.service;

import com.quespot.domain.item.converter.ItemConverter;
import com.quespot.domain.item.dto.res.UserItemResponseDTO;
import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.exception.ItemException;
import com.quespot.domain.item.exception.code.ItemErrorCode;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserItemService {

    private final UserItemRepository userItemRepository;
    private final EquipSlotLimitRepository equipSlotLimitRepository;

    @Transactional(readOnly = true)
    public List<UserItemResponseDTO> getMyItems(Long userId) {
        return userItemRepository.findByUserIdOrderByPurchasedAtDesc(userId).stream()
                .map(ItemConverter::toUserItemResponseDTO)
                .toList();
    }

    @Transactional
    public void equip(Long userId, Long itemId) {
        UserItem userItem = userItemRepository.findByUserIdAndItem_Id(userId, itemId)
                .orElseThrow(() -> new ItemException(ItemErrorCode.ITEM_NOT_OWNED));

        if (userItem.getIsEquipped()) {
            return;
        }

        ItemCategory category = userItem.getItem().getCategory();
        int maxEquip = equipSlotLimitRepository.findById(category)
                .map(EquipSlotLimit::getMaxEquip)
                .orElse(1);

        List<UserItem> equippedInCategory = userItemRepository
                .findByUserIdAndItem_CategoryAndIsEquippedTrueOrderByEquippedAtAsc(userId, category);

        int slotsNeeded = equippedInCategory.size() + 1 - maxEquip;
        for (int i = 0; i < slotsNeeded; i++) {
            equippedInCategory.get(i).unequip();
        }

        userItem.equip();
    }

    @Transactional
    public void unequip(Long userId, Long itemId) {
        userItemRepository.findByUserIdAndItem_Id(userId, itemId)
                .ifPresent(UserItem::unequip);
    }
}
