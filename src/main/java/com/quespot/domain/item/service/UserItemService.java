package com.quespot.domain.item.service;

import com.quespot.domain.item.converter.ItemConverter;
import com.quespot.domain.item.dto.res.EquippedItemResponseDTO;
import com.quespot.domain.item.dto.res.QuestyResponseDTO;
import com.quespot.domain.item.dto.res.UserItemResponseDTO;
import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;
import com.quespot.domain.item.exception.ItemException;
import com.quespot.domain.item.exception.code.ItemErrorCode;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserItemService {

    private final UserItemRepository userItemRepository;
    private final EquipSlotLimitRepository equipSlotLimitRepository;

    @Transactional(readOnly = true)
    public List<UserItemResponseDTO> getMyItems(Long userId) {
        return userItemRepository.findAllByUserIdWithItem(userId).stream()
                .map(ItemConverter::toUserItemResponseDTO)
                .toList();
    }

    // 기존 join fetch 쿼리 한 번으로 장착 목록·보유 수·최고 등급을 모두 계산한다(#50).
    @Transactional(readOnly = true)
    public QuestyResponseDTO getQuesty(Long userId) {
        List<UserItem> owned = userItemRepository.findAllByUserIdWithItem(userId);
        List<EquippedItemResponseDTO> equipped = owned.stream()
                .filter(UserItem::getIsEquipped)
                .sorted(Comparator.comparing((UserItem ui) -> ui.getItem().getCategory())
                        .thenComparing(UserItem::getEquippedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ItemConverter::toEquippedItemResponseDTO)
                .toList();
        ItemRarity highest = owned.stream()
                .map(ui -> ui.getItem().getRarity())
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new QuestyResponseDTO(equipped, owned.size(), highest);
    }

    @Transactional
    public void equip(Long userId, Long itemId) {
        UserItem target = userItemRepository.findByUserIdAndItem_Id(userId, itemId)
                .orElseThrow(() -> new ItemException(ItemErrorCode.ITEM_NOT_OWNED));

        if (target.getIsEquipped()) {
            return;
        }

        ItemCategory category = target.getItem().getCategory();

        // 같은 유저·카테고리 아이템 전체를 잠가서, 동시에 들어온 다른 장착 요청과 슬롯 한도 검사가 겹치지 않게 한다.
        List<UserItem> categoryItems = userItemRepository.lockAllByUserIdAndItem_Category(userId, category);

        int maxEquip = equipSlotLimitRepository.findById(category)
                .map(EquipSlotLimit::getMaxEquip)
                .orElse(1);

        List<UserItem> equippedInCategory = categoryItems.stream()
                .filter(UserItem::getIsEquipped)
                .sorted(Comparator.comparing(UserItem::getEquippedAt))
                .toList();

        int slotsNeeded = equippedInCategory.size() + 1 - maxEquip;
        for (int i = 0; i < slotsNeeded; i++) {
            equippedInCategory.get(i).unequip();
        }

        target.equip();
    }

    @Transactional
    public void unequip(Long userId, Long itemId) {
        userItemRepository.findByUserIdAndItem_Id(userId, itemId)
                .ifPresent(UserItem::unequip);
    }
}
