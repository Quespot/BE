package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.req.UpdateNotificationSettingRequestDTO;
import com.quespot.domain.notification.dto.res.NotificationSettingResponseDTO;
import com.quespot.domain.notification.entity.NotificationSetting;
import com.quespot.domain.notification.repository.NotificationSettingRepository;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSettingServiceTest {

    private NotificationSettingRepository notificationSettingRepository;
    private UserRepository userRepository;
    private NotificationSettingService notificationSettingService;

    @BeforeEach
    void setUp() {
        notificationSettingRepository = mock(NotificationSettingRepository.class);
        userRepository = mock(UserRepository.class);
        notificationSettingService = new NotificationSettingService(
                notificationSettingRepository,
                userRepository
        );
    }

    @Test
    void returnsDisabledWhenSettingDoesNotExist() {
        Long userId = 1L;
        User user = activeUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationSettingRepository.findById(userId)).thenReturn(Optional.empty());

        NotificationSettingResponseDTO response = notificationSettingService.getSetting(userId);

        assertThat(response.pushEnabled()).isFalse();
        verify(notificationSettingRepository, never()).save(any(NotificationSetting.class));
    }

    @Test
    void returnsSavedSetting() {
        Long userId = 1L;
        User user = activeUser(userId);
        NotificationSetting setting = NotificationSetting.create(user, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationSettingRepository.findById(userId)).thenReturn(Optional.of(setting));

        NotificationSettingResponseDTO response = notificationSettingService.getSetting(userId);

        assertThat(response.pushEnabled()).isTrue();
    }

    @Test
    void createsSettingOnFirstUpdate() {
        Long userId = 1L;
        User user = activeUser(userId);
        UpdateNotificationSettingRequestDTO request = new UpdateNotificationSettingRequestDTO(true);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(notificationSettingRepository.findById(userId)).thenReturn(Optional.empty());
        when(notificationSettingRepository.save(any(NotificationSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingResponseDTO response = notificationSettingService.updateSetting(userId, request);

        assertThat(response.pushEnabled()).isTrue();
        verify(notificationSettingRepository).save(any(NotificationSetting.class));
    }

    @Test
    void updatesExistingSetting() {
        Long userId = 1L;
        User user = activeUser(userId);
        NotificationSetting setting = NotificationSetting.create(user, true);
        UpdateNotificationSettingRequestDTO request = new UpdateNotificationSettingRequestDTO(false);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(notificationSettingRepository.findById(userId)).thenReturn(Optional.of(setting));
        when(notificationSettingRepository.save(setting)).thenReturn(setting);

        NotificationSettingResponseDTO response = notificationSettingService.updateSetting(userId, request);

        assertThat(response.pushEnabled()).isFalse();
        assertThat(setting.isPushEnabled()).isFalse();
        verify(notificationSettingRepository).save(setting);
    }

    private User activeUser(Long userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        return user;
    }
}
