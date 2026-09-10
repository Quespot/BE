package com.quespot.domain.item.controller;

import com.quespot.domain.item.dto.res.QuestyResponseDTO;
import com.quespot.domain.item.exception.code.ItemSuccessCode;
import com.quespot.domain.item.service.UserItemService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// UserItemController는 클래스 레벨 @RequestMapping("/api/users/me/items")라
// 다른 경로를 붙일 수 없어 별도 컨트롤러로 둔다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/questy")
@Tag(name = "Item", description = "퀘스티 꾸미기 API")
public class QuestyController {

    private final UserItemService userItemService;

    @GetMapping
    @Operation(
            summary = "내 퀘스티 조회",
            description = "홈 화면에서 꾸민 퀘스티를 그리기 위한 장착 아이템(레이어 URL)과 보유 수, 최고 등급을 조회한다. "
                    + "서버는 이미지를 합성하지 않는다 — 프론트가 레이어를 겹쳐 그린다."
    )
    public ApiResponse<QuestyResponseDTO> getQuesty(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.of(ItemSuccessCode.QUESTY_FOUND, userItemService.getQuesty(principal.userId()));
    }
}
