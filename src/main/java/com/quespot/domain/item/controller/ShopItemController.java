package com.quespot.domain.item.controller;

import com.quespot.domain.item.dto.res.PurchaseItemResponseDTO;
import com.quespot.domain.item.dto.res.ShopItemResponseDTO;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.exception.code.ItemSuccessCode;
import com.quespot.domain.item.service.ItemPurchaseService;
import com.quespot.domain.item.service.ShopItemService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop/items")
@Tag(name = "Item", description = "퀘스티 꾸미기 API")
public class ShopItemController {

    private final ShopItemService shopItemService;
    private final ItemPurchaseService itemPurchaseService;

    @PostMapping("/{itemId}/purchase")
    @Operation(
            summary = "아이템 구매",
            description = "포인트를 차감하고 아이템을 지급한다. 자동 장착하지 않는다. "
                    + "이미 보유한 아이템은 400, 잔액 부족은 400, 판매 중이 아니면 404. "
                    + "가격 0인 아이템은 차감·원장 기록 없이 지급된다."
    )
    public ApiResponse<PurchaseItemResponseDTO> purchase(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @Positive Long itemId
    ) {
        return ApiResponse.of(ItemSuccessCode.ITEM_PURCHASED, itemPurchaseService.purchase(principal.userId(), itemId));
    }

    @GetMapping
    @Operation(
            summary = "상점 아이템 목록",
            description = "판매 중인 상점 아이템 목록을 조회한다. category를 지정하면 해당 카테고리만 반환한다."
    )
    public ApiResponse<List<ShopItemResponseDTO>> getShopItems(
            @RequestParam(required = false) ItemCategory category
    ) {
        return ApiResponse.of(ItemSuccessCode.SHOP_ITEMS_FOUND, shopItemService.getShopItems(category));
    }
}
