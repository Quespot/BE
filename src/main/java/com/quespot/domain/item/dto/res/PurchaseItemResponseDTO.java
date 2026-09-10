package com.quespot.domain.item.dto.res;

// balance는 구매 직후 실제 잔액 — 화면이 포인트를 다시 조회하지 않고 바로 반영하게.
// 무료 아이템(가격 0)은 차감이 없으므로 구매 전과 같은 잔액이 내려간다.
public record PurchaseItemResponseDTO(
        Long itemId,
        int paidPoint,
        int balance
) {
}
