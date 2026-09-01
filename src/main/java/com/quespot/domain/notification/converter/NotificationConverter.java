package com.quespot.domain.notification.converter;

import com.quespot.domain.notification.dto.res.RegisterFcmTokenResponseDTO;
import com.quespot.domain.notification.entity.FcmToken;

public final class NotificationConverter {

    private NotificationConverter() {
    }

    public static RegisterFcmTokenResponseDTO toRegisterFcmTokenResponseDTO(FcmToken fcmToken) {
        return new RegisterFcmTokenResponseDTO(fcmToken.getId(), fcmToken.getDeviceType());
    }
}
