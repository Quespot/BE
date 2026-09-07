package com.quespot.domain.notification.converter;

import com.quespot.domain.notification.dto.res.RegisterFcmTokenResponseDTO;
import com.quespot.domain.notification.dto.res.NotificationSettingResponseDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.entity.NotificationSetting;

public final class NotificationConverter {

    private NotificationConverter() {
    }

    public static RegisterFcmTokenResponseDTO toRegisterFcmTokenResponseDTO(FcmToken fcmToken) {
        return new RegisterFcmTokenResponseDTO(fcmToken.getId(), fcmToken.getDeviceType());
    }

    public static NotificationSettingResponseDTO toNotificationSettingResponseDTO(
            NotificationSetting setting
    ) {
        return new NotificationSettingResponseDTO(setting.isPushEnabled());
    }

    public static NotificationSettingResponseDTO toDefaultNotificationSettingResponseDTO() {
        return new NotificationSettingResponseDTO(false);
    }
}
