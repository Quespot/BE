package com.quespot.domain.notification.controller;

import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.dto.res.RegisterFcmTokenResponseDTO;
import com.quespot.domain.notification.exception.code.NotificationSuccessCode;
import com.quespot.domain.notification.service.FcmTokenService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/fcm-tokens")
@Tag(name = "Notification", description = "알림/FCM API")
public class NotificationController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    @Operation(
            summary = "FCM 토큰 등록",
            description = "로그인한 사용자의 기기 FCM 토큰을 등록한다. 이미 등록된 토큰이면 소유자를 갱신한다."
    )
    public ApiResponse<RegisterFcmTokenResponseDTO> registerToken(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RegisterFcmTokenRequestDTO request
    ) {
        RegisterFcmTokenResponseDTO response = fcmTokenService.registerToken(principal.userId(), request);

        return ApiResponse.of(NotificationSuccessCode.FCM_TOKEN_REGISTERED, response);
    }

    @DeleteMapping
    @Operation(
            summary = "FCM 토큰 해제",
            description = "로그인한 사용자의 기기 FCM 토큰을 삭제한다. 존재하지 않아도 200을 반환한다."
    )
    public ApiResponse<Void> unregisterToken(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String token
    ) {
        fcmTokenService.unregisterToken(principal.userId(), token);

        return ApiResponse.<Void>of(NotificationSuccessCode.FCM_TOKEN_UNREGISTERED, null);
    }
}
