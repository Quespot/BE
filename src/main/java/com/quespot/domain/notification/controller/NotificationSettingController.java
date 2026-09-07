package com.quespot.domain.notification.controller;

import com.quespot.domain.notification.dto.req.UpdateNotificationSettingRequestDTO;
import com.quespot.domain.notification.dto.res.NotificationSettingResponseDTO;
import com.quespot.domain.notification.exception.code.NotificationSuccessCode;
import com.quespot.domain.notification.service.NotificationSettingService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/notification-settings")
@Tag(name = "Notification", description = "알림/FCM API")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @GetMapping
    @Operation(
            summary = "알림 설정 조회",
            description = "로그인한 사용자의 알림 수신 여부를 조회합니다. 설정 이력이 없으면 비활성화 상태를 반환합니다."
    )
    public ApiResponse<NotificationSettingResponseDTO> getSetting(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        NotificationSettingResponseDTO response = notificationSettingService.getSetting(principal.userId());

        return ApiResponse.of(NotificationSuccessCode.NOTIFICATION_SETTING_FOUND, response);
    }

    @PatchMapping
    @Operation(
            summary = "알림 설정 변경",
            description = "로그인한 사용자의 알림 수신 여부를 변경합니다. 최초 변경 시 알림 설정을 생성합니다."
    )
    public ApiResponse<NotificationSettingResponseDTO> updateSetting(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateNotificationSettingRequestDTO request
    ) {
        NotificationSettingResponseDTO response = notificationSettingService.updateSetting(
                principal.userId(),
                request
        );

        return ApiResponse.of(NotificationSuccessCode.NOTIFICATION_SETTING_UPDATED, response);
    }
}
