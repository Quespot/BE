package com.quespot.domain.notification.controller;

import com.quespot.domain.notification.dto.res.NotificationListResponseDTO;
import com.quespot.domain.notification.dto.res.UnreadCountResponseDTO;
import com.quespot.domain.notification.exception.code.NotificationSuccessCode;
import com.quespot.domain.notification.service.NotificationQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "알림/FCM API")
@Validated
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    @Operation(
            summary = "인앱 알림 목록 조회",
            description = """
                    로그인한 사용자의 알림을 최신순으로 돌려준다. 커서 페이징: 첫 요청은 cursor 없이,
                    응답의 nextCursor(숫자 id)를 다음 요청의 cursor에 그대로 넣는다. hasNext=false면 마지막 페이지이고
                    nextCursor는 null. 결과가 없으면 notifications는 빈 배열이다.
                    referenceType=MISSION이면 referenceId는 미션 id라 미션 상세로 딥링크할 수 있다.
                    """
    )
    public ApiResponse<NotificationListResponseDTO> getNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size
    ) {
        return ApiResponse.of(
                NotificationSuccessCode.NOTIFICATION_LIST_FOUND,
                notificationQueryService.getNotifications(principal.userId(), cursor, size)
        );
    }

    @GetMapping("/unread-count")
    @Operation(summary = "미읽음 알림 개수 조회", description = "종 아이콘 배지용. 읽지 않은 알림 개수를 돌려준다.")
    public ApiResponse<UnreadCountResponseDTO> countUnread(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                NotificationSuccessCode.NOTIFICATION_UNREAD_COUNT_FOUND,
                notificationQueryService.countUnread(principal.userId())
        );
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(
            summary = "알림 읽음 처리",
            description = "본인 알림 하나를 읽음 처리한다. 이미 읽은 알림이어도 200을 반환한다(멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "NOTIFICATION_404_001 알림 없음(남의 알림 포함) — 목록 새로고침",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> markAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable @Positive Long notificationId
    ) {
        notificationQueryService.markAsRead(principal.userId(), notificationId);
        return ApiResponse.<Void>of(NotificationSuccessCode.NOTIFICATION_READ, null);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "알림 전체 읽음 처리", description = "본인의 읽지 않은 알림을 전부 읽음 처리한다. 대상이 없어도 200.")
    public ApiResponse<Void> markAllAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        notificationQueryService.markAllAsRead(principal.userId());
        return ApiResponse.<Void>of(NotificationSuccessCode.NOTIFICATION_ALL_READ, null);
    }
}
