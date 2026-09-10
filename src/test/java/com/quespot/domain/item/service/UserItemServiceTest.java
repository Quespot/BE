package com.quespot.domain.item.service;

import com.quespot.domain.item.dto.res.UserItemResponseDTO;
import com.quespot.domain.item.entity.EquipSlotLimit;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;
import com.quespot.domain.item.exception.ItemException;
import com.quespot.domain.item.exception.code.ItemErrorCode;
import com.quespot.domain.item.repository.EquipSlotLimitRepository;
import com.quespot.domain.item.repository.UserItemRepository;
import com.quespot.domain.item.dto.res.EquippedItemResponseDTO;
import com.quespot.domain.item.dto.res.QuestyResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserItemServiceTest {

    private UserItemRepository userItemRepository;
    private EquipSlotLimitRepository equipSlotLimitRepository;
    private UserItemService userItemService;

    @BeforeEach
    void setUp() {
        userItemRepository = mock(UserItemRepository.class);
        equipSlotLimitRepository = mock(EquipSlotLimitRepository.class);
        userItemService = new UserItemService(userItemRepository, equipSlotLimitRepository);
    }

    private ShopItem shopItem(Long id, ItemCategory category) {
        ShopItem item = ShopItem.seed("CODE", "이름", category, ItemRarity.NORMAL, 0, false, false);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    @Test
    void equipsOwnedItemThatIsNotYetEquipped() {
        Long userId = 1L;
        Long itemId = 10L;
        UserItem userItem = UserItem.acquire(userId, shopItem(itemId, ItemCategory.HAT));

        when(userItemRepository.findByUserIdAndItem_Id(userId, itemId)).thenReturn(Optional.of(userItem));
        when(equipSlotLimitRepository.findById(ItemCategory.HAT))
                .thenReturn(Optional.of(EquipSlotLimit.seed(ItemCategory.HAT, 1, 1)));
        when(userItemRepository.lockAllByUserIdAndItem_Category(userId, ItemCategory.HAT))
                .thenReturn(List.of(userItem));

        userItemService.equip(userId, itemId);

        assertThat(userItem.getIsEquipped()).isTrue();
    }

    @Test
    void equipIsIdempotentWhenAlreadyEquipped() {
        Long userId = 1L;
        Long itemId = 10L;
        UserItem userItem = UserItem.acquire(userId, shopItem(itemId, ItemCategory.HAT));
        userItem.equip();

        when(userItemRepository.findByUserIdAndItem_Id(userId, itemId)).thenReturn(Optional.of(userItem));

        userItemService.equip(userId, itemId);

        assertThat(userItem.getIsEquipped()).isTrue();
        verifyNoInteractions(equipSlotLimitRepository);
    }

    @Test
    void equipSwapsOldestEquippedItemWhenSlotLimitReached() {
        Long userId = 1L;
        UserItem currentlyEquipped = UserItem.acquire(userId, shopItem(1L, ItemCategory.HAT));
        currentlyEquipped.equip();
        UserItem newItem = UserItem.acquire(userId, shopItem(2L, ItemCategory.HAT));

        when(userItemRepository.findByUserIdAndItem_Id(userId, 2L)).thenReturn(Optional.of(newItem));
        when(equipSlotLimitRepository.findById(ItemCategory.HAT))
                .thenReturn(Optional.of(EquipSlotLimit.seed(ItemCategory.HAT, 1, 1)));
        when(userItemRepository.lockAllByUserIdAndItem_Category(userId, ItemCategory.HAT))
                .thenReturn(List.of(currentlyEquipped, newItem));

        userItemService.equip(userId, 2L);

        assertThat(currentlyEquipped.getIsEquipped()).isFalse();
        assertThat(newItem.getIsEquipped()).isTrue();
    }

    @Test
    void equipThrowsWhenItemNotOwned() {
        Long userId = 1L;
        Long itemId = 99L;
        when(userItemRepository.findByUserIdAndItem_Id(userId, itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userItemService.equip(userId, itemId))
                .isInstanceOf(ItemException.class)
                .extracting(exception -> ((ItemException) exception).getErrorCode())
                .isEqualTo(ItemErrorCode.ITEM_NOT_OWNED);
    }

    @Test
    void unequipsOwnedEquippedItem() {
        Long userId = 1L;
        Long itemId = 10L;
        UserItem userItem = UserItem.acquire(userId, shopItem(itemId, ItemCategory.HAT));
        userItem.equip();

        when(userItemRepository.findByUserIdAndItem_Id(userId, itemId)).thenReturn(Optional.of(userItem));

        userItemService.unequip(userId, itemId);

        assertThat(userItem.getIsEquipped()).isFalse();
    }

    @Test
    void unequipIsIdempotentWhenItemNotOwned() {
        Long userId = 1L;
        Long itemId = 99L;
        when(userItemRepository.findByUserIdAndItem_Id(userId, itemId)).thenReturn(Optional.empty());

        userItemService.unequip(userId, itemId);
    }

    @Test
    void listsOwnedItemsOrderedByPurchasedAtDescending() {
        Long userId = 1L;
        UserItem userItem = UserItem.acquire(userId, shopItem(10L, ItemCategory.HAT));

        when(userItemRepository.findAllByUserIdWithItem(userId)).thenReturn(List.of(userItem));

        List<UserItemResponseDTO> result = userItemService.getMyItems(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemId()).isEqualTo(10L);
    }

    @Test
    void questyReturnsOnlyEquippedItemsWithOwnedCountAndHighestRarity() {
        ShopItem hat = ShopItem.seed("HAT", "탐험가 모자", ItemCategory.HAT, ItemRarity.NORMAL, 0, true, false);
        ReflectionTestUtils.setField(hat, "id", 1L);
        ShopItem crown = ShopItem.seed("CROWN", "황금 왕관", ItemCategory.HAT, ItemRarity.LEGENDARY, 300, false, false);
        ReflectionTestUtils.setField(crown, "id", 2L);
        ShopItem scarf = ShopItem.seed("SCARF", "파란 스카프", ItemCategory.ACCESSORY, ItemRarity.RARE, 0, true, false);
        ReflectionTestUtils.setField(scarf, "id", 3L);
        UserItem ownedHat = UserItem.acquire(1L, hat);
        ownedHat.equip();
        UserItem ownedCrown = UserItem.acquire(1L, crown);          // 미장착
        UserItem ownedScarf = UserItem.acquire(1L, scarf);
        ownedScarf.equip();
        when(userItemRepository.findAllByUserIdWithItem(1L)).thenReturn(List.of(ownedScarf, ownedCrown, ownedHat));

        QuestyResponseDTO result = userItemService.getQuesty(1L);

        assertThat(result.ownedItemCount()).isEqualTo(3);
        assertThat(result.highestRarity()).isEqualTo(ItemRarity.LEGENDARY);
        assertThat(result.equippedItems()).extracting(EquippedItemResponseDTO::itemId).containsExactly(1L, 3L); // HAT -> ACCESSORY
        assertThat(result.equippedItems().get(0).name()).isEqualTo("탐험가 모자");
    }

    @Test
    void questyOfUserWithNoItemsHasNullHighestRarity() {
        when(userItemRepository.findAllByUserIdWithItem(1L)).thenReturn(List.of());

        QuestyResponseDTO result = userItemService.getQuesty(1L);

        assertThat(result.equippedItems()).isEmpty();
        assertThat(result.ownedItemCount()).isZero();
        assertThat(result.highestRarity()).isNull();
    }
}
