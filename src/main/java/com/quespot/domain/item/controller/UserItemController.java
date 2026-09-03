package com.quespot.domain.item.controller;

import com.quespot.domain.item.dto.res.UserItemResponseDTO;
import com.quespot.domain.item.exception.code.ItemSuccessCode;
import com.quespot.domain.item.service.UserItemService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/items")
@Tag(name = "Item", description = "퀘스티 꾸미기 API")
public class UserItemController {

    private final UserItemService userItemService;

    @GetMapping
    @Operation(
            summary = "내 보유 아이템 목록",
            description = "로그인한 사용자가 보유한 아이템 목록을 장착 상태와 함께 조회한다."
    )
    public ApiResponse<List<UserItemResponseDTO>> getMyItems(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(ItemSuccessCode.MY_ITEMS_FOUND, userItemService.getMyItems(principal.userId()));
    }

    @PostMapping("/{itemId}/equip")
    @Operation(
            summary = "아이템 장착",
            description = "보유한 아이템을 장착한다. 이미 장착 중이면 그대로 200을 반환한다. "
                    + "같은 카테고리 슬롯이 가득 차면 가장 오래 장착한 아이템부터 자동 해제한다."
    )
    public ApiResponse<Void> equip(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long itemId
    ) {
        userItemService.equip(principal.userId(), itemId);

        return ApiResponse.<Void>of(ItemSuccessCode.ITEM_EQUIPPED, null);
    }

    @DeleteMapping("/{itemId}/equip")
    @Operation(
            summary = "아이템 해제",
            description = "장착한 아이템을 해제한다. 보유하지 않았거나 이미 해제 상태여도 200을 반환한다."
    )
    public ApiResponse<Void> unequip(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long itemId
    ) {
        userItemService.unequip(principal.userId(), itemId);

        return ApiResponse.<Void>of(ItemSuccessCode.ITEM_UNEQUIPPED, null);
    }
}
