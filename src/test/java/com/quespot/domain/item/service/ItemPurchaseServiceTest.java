package com.quespot.domain.item.service;

import com.quespot.domain.item.dto.res.PurchaseItemResponseDTO;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.enums.ItemRarity;
import com.quespot.domain.item.exception.ItemException;
import com.quespot.domain.item.exception.code.ItemErrorCode;
import com.quespot.domain.item.repository.ShopItemRepository;
import com.quespot.domain.item.repository.UserItemRepository;
import com.quespot.domain.reward.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemPurchaseServiceTest {

    private ShopItemRepository shopItemRepository;
    private UserItemRepository userItemRepository;
    private PointService pointService;
    private ItemPurchaseService service;

    @BeforeEach
    void setUp() {
        shopItemRepository = mock(ShopItemRepository.class);
        userItemRepository = mock(UserItemRepository.class);
        pointService = mock(PointService.class);
        service = new ItemPurchaseService(shopItemRepository, userItemRepository, pointService);
        when(userItemRepository.save(any(UserItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ShopItem item(Long id, int price) {
        ShopItem item = ShopItem.seed("GOLDEN_CROWN", "황금 왕관", ItemCategory.HAT, ItemRarity.LEGENDARY, price, false, false);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    @Test
    void purchaseDebitsPointsThenGrantsItemWithoutEquipping() {
        when(shopItemRepository.findByIdAndIsActiveTrue(5L)).thenReturn(Optional.of(item(5L, 300)));
        when(userItemRepository.existsByUserIdAndItem_Id(7L, 5L)).thenReturn(false);
        when(pointService.debit(7L, 300, "ITEM_PURCHASE", "SHOP_ITEM", 5L, "황금 왕관 구매")).thenReturn(50);

        PurchaseItemResponseDTO result = service.purchase(7L, 5L);

        assertThat(result).isEqualTo(new PurchaseItemResponseDTO(5L, 300, 50));
        ArgumentCaptor<UserItem> saved = ArgumentCaptor.forClass(UserItem.class);
        verify(userItemRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(7L);
        assertThat(saved.getValue().getIsEquipped()).isFalse();
    }

    @Test
    void purchaseOfFreeItemSkipsLedger() {
        when(shopItemRepository.findByIdAndIsActiveTrue(5L)).thenReturn(Optional.of(item(5L, 0)));
        when(userItemRepository.existsByUserIdAndItem_Id(7L, 5L)).thenReturn(false);

        PurchaseItemResponseDTO result = service.purchase(7L, 5L);

        assertThat(result.paidPoint()).isZero();
        verifyNoInteractions(pointService);
        verify(userItemRepository).save(any(UserItem.class));
    }

    @Test
    void purchaseThrows404WhenItemMissingOrInactive() {
        when(shopItemRepository.findByIdAndIsActiveTrue(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchase(7L, 5L))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode())
                .isEqualTo(ItemErrorCode.ITEM_NOT_FOUND);
        verifyNoInteractions(pointService);
    }

    @Test
    void purchaseThrows400WhenAlreadyOwnedAndDoesNotDebit() {
        when(shopItemRepository.findByIdAndIsActiveTrue(5L)).thenReturn(Optional.of(item(5L, 300)));
        when(userItemRepository.existsByUserIdAndItem_Id(7L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.purchase(7L, 5L))
                .isInstanceOf(ItemException.class)
                .extracting(e -> ((ItemException) e).getErrorCode())
                .isEqualTo(ItemErrorCode.ITEM_ALREADY_OWNED);
        verify(pointService, never()).debit(anyLong(), anyInt(), anyString(), anyString(), any(), anyString());
        verify(userItemRepository, never()).save(any());
    }
}
