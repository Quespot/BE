package com.quespot.domain.item.service;

import com.quespot.domain.item.dto.res.PurchaseItemResponseDTO;
import com.quespot.domain.item.entity.ShopItem;
import com.quespot.domain.item.entity.UserItem;
import com.quespot.domain.item.exception.ItemException;
import com.quespot.domain.item.exception.code.ItemErrorCode;
import com.quespot.domain.item.repository.ShopItemRepository;
import com.quespot.domain.item.repository.UserItemRepository;
import com.quespot.domain.reward.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 구매 = 포인트 차감 + user_items 지급, 하나의 트랜잭션(REQUIRED). 차감은
// 조건부 UPDATE(PointService.debit), 이중 지급은 UNIQUE(user_id, item_id)가 막는다
// — 동시 이중 구매에서 한쪽은 UNIQUE 위반으로 차감까지 롤백된다. 이미 보유는
// 조용히 성공시키지 않고 400 — 포인트가 걸려 있어서 좋아요와 다르다.
// 구매 후 자동 장착 없음(카테고리 슬롯 제한과 충돌).
@Service
@RequiredArgsConstructor
public class ItemPurchaseService {

    private final ShopItemRepository shopItemRepository;
    private final UserItemRepository userItemRepository;
    private final PointService pointService;

    @Transactional
    public PurchaseItemResponseDTO purchase(Long userId, Long itemId) {
        ShopItem item = shopItemRepository.findByIdAndIsActiveTrue(itemId)
                .orElseThrow(() -> new ItemException(ItemErrorCode.ITEM_NOT_FOUND));
        if (userItemRepository.existsByUserIdAndItem_Id(userId, itemId)) {
            throw new ItemException(ItemErrorCode.ITEM_ALREADY_OWNED);
        }
        int price = item.getPrice();
        // 가격 0(기본 아이템 등)은 원장에 0원 행을 남기지 않는다(CLAUDE.md) —
        // 활동 기록도 없이 지급만 한다. 응답 balance는 화면이 바로 반영하는 값이라
        // 무료여도 실제 잔액을 돌려준다(0으로 고정하면 잔액이 0으로 보이는 버그).
        int balance = price > 0
                ? pointService.debit(userId, price, "ITEM_PURCHASE", "SHOP_ITEM", itemId, item.getName() + " 구매")
                : pointService.getPoints(userId).balance();
        userItemRepository.save(UserItem.acquire(userId, item));
        return new PurchaseItemResponseDTO(itemId, price, balance);
    }
}
