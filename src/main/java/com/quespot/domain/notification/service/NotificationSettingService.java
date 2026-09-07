package com.quespot.domain.notification.service;

import com.quespot.domain.notification.converter.NotificationConverter;
import com.quespot.domain.notification.dto.req.UpdateNotificationSettingRequestDTO;
import com.quespot.domain.notification.dto.res.NotificationSettingResponseDTO;
import com.quespot.domain.notification.entity.NotificationSetting;
import com.quespot.domain.notification.repository.NotificationSettingRepository;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    // 알림 설정 조회 로직
    @Transactional(readOnly = true)
    public NotificationSettingResponseDTO getSetting(Long userId) {
        User user = findActiveUser(userId);

        return notificationSettingRepository.findById(user.getId())
                .map(NotificationConverter::toNotificationSettingResponseDTO)
                .orElseGet(NotificationConverter::toDefaultNotificationSettingResponseDTO);
    }

    // 알림 설정 변경 로직
    @Transactional
    public NotificationSettingResponseDTO updateSetting(
            Long userId,
            UpdateNotificationSettingRequestDTO request
    ) {
        User user = findActiveUserForUpdate(userId);
        NotificationSetting setting = notificationSettingRepository.findById(user.getId())
                .orElseGet(() -> NotificationSetting.create(user, request.pushEnabled()));

        setting.updatePushEnabled(request.pushEnabled());

        return NotificationConverter.toNotificationSettingResponseDTO(
                notificationSettingRepository.save(setting)
        );
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN));
    }

    private User findActiveUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN));
    }
}
