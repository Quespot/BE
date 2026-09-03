package com.quespot.domain.item.controller;

import com.quespot.domain.item.dto.res.ShopItemResponseDTO;
import com.quespot.domain.item.enums.ItemCategory;
import com.quespot.domain.item.exception.code.ItemSuccessCode;
import com.quespot.domain.item.service.ShopItemService;
import com.quespot.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop/items")
@Tag(name = "Item", description = "퀘스티 꾸미기 API")
public class ShopItemController {

    private final ShopItemService shopItemService;

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
