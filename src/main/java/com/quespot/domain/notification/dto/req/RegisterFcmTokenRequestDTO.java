package com.quespot.domain.notification.dto.req;

import com.quespot.domain.notification.enums.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegisterFcmTokenRequestDTO(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String token,

        @NotNull(message = "기기 타입은 필수입니다.")
        DeviceType deviceType,

        @Schema(description = "기기의 현재 위도. longitude와 함께 보내거나 둘 다 생략한다. 보내면 주변 미션 추천 알림의 기준 위치가 된다", nullable = true)
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,

        @Schema(description = "기기의 현재 경도. latitude와 함께 보내거나 둘 다 생략한다", nullable = true)
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude
) {

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    @AssertTrue(message = "위도와 경도는 함께 보내거나 함께 생략해야 합니다.")
    @Schema(hidden = true)
    public boolean isLocationComplete() {
        return (latitude == null) == (longitude == null);
    }
}
