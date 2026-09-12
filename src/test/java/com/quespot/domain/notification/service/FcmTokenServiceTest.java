package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.dto.res.RegisterFcmTokenResponseDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.DeviceType;
import com.quespot.domain.notification.exception.NotificationException;
import com.quespot.domain.notification.exception.code.NotificationErrorCode;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmTokenServiceTest {

    private FcmTokenRepository fcmTokenRepository;
    private FcmTokenWriter fcmTokenWriter;
    private FcmTokenService fcmTokenService;

    @BeforeEach
    void setUp() {
        fcmTokenRepository = mock(FcmTokenRepository.class);
        fcmTokenWriter = mock(FcmTokenWriter.class);
        fcmTokenService = new FcmTokenService(fcmTokenRepository, fcmTokenWriter);
    }

    @Test
    void registersNewToken() {
        Long userId = 1L;
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO("token-a", DeviceType.ANDROID, null, null);
        FcmToken savedToken = FcmToken.register(userId, request.token(), request.deviceType());
        ReflectionTestUtils.setField(savedToken, "id", 10L);

        when(fcmTokenRepository.findByToken(request.token())).thenReturn(Optional.empty());
        when(fcmTokenWriter.saveNewToken(userId, request)).thenReturn(savedToken);

        RegisterFcmTokenResponseDTO response = fcmTokenService.registerToken(userId, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.deviceType()).isEqualTo(DeviceType.ANDROID);
        verify(fcmTokenWriter, times(1)).saveNewToken(userId, request);
        verify(fcmTokenRepository, never()).saveAndFlush(any(FcmToken.class));
    }

    @Test
    void reassignsExistingTokenToNewOwner() {
        Long previousOwnerId = 1L;
        Long newOwnerId = 2L;
        String token = "token-shared-device";
        FcmToken existingToken = FcmToken.register(previousOwnerId, token, DeviceType.ANDROID);
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO(token, DeviceType.IOS, null, null);

        when(fcmTokenRepository.findByToken(token)).thenReturn(Optional.of(existingToken));

        RegisterFcmTokenResponseDTO response = fcmTokenService.registerToken(newOwnerId, request);

        assertThat(existingToken.getUserId()).isEqualTo(newOwnerId);
        assertThat(existingToken.getDeviceType()).isEqualTo(DeviceType.IOS);
        assertThat(response.deviceType()).isEqualTo(DeviceType.IOS);
        verify(fcmTokenWriter, never()).saveNewToken(any(), any());
    }

    @Test
    void fallsBackToExistingTokenOnConcurrentInsertRace() {
        Long userId = 1L;
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO("token-race", DeviceType.WEB, null, null);
        FcmToken raceWinnerToken = FcmToken.register(userId, request.token(), request.deviceType());

        when(fcmTokenRepository.findByToken(request.token())).thenReturn(Optional.empty());
        when(fcmTokenWriter.saveNewToken(userId, request))
                .thenThrow(new DataIntegrityViolationException("duplicate token"));
        // 재조회는 같은 트랜잭션(옛 스냅샷)이 아니라 writer의 REQUIRES_NEW에서 한다.
        when(fcmTokenWriter.reassignExisting(userId, request)).thenReturn(Optional.of(raceWinnerToken));

        RegisterFcmTokenResponseDTO response = fcmTokenService.registerToken(userId, request);

        assertThat(response.deviceType()).isEqualTo(DeviceType.WEB);
        verify(fcmTokenRepository, times(1)).findByToken(request.token());
    }

    @Test
    void clearsPreviousLocationWhenOwnerChangesWithoutNewLocation() {
        FcmToken existing = FcmToken.register(1L, "token-owner", DeviceType.ANDROID);
        existing.updateLocation(new BigDecimal("37.1"), new BigDecimal("127.1"), LocalDateTime.of(2026, 9, 1, 10, 0));
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO("token-owner", DeviceType.ANDROID, null, null);

        when(fcmTokenRepository.findByToken("token-owner")).thenReturn(Optional.of(existing));

        fcmTokenService.registerToken(2L, request);

        assertThat(existing.getUserId()).isEqualTo(2L);
        assertThat(existing.getLastLatitude()).isNull();
        assertThat(existing.getLastLongitude()).isNull();
        assertThat(existing.getLocatedAt()).isNull();
    }

    @Test
    void throwsNotificationExceptionWhenConcurrentInsertWinnerCannotBeFound() {
        Long userId = 1L;
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO("token-failed", DeviceType.WEB, null, null);

        when(fcmTokenRepository.findByToken(request.token())).thenReturn(Optional.empty());
        when(fcmTokenWriter.saveNewToken(userId, request))
                .thenThrow(new DataIntegrityViolationException("duplicate token"));

        assertThatThrownBy(() -> fcmTokenService.registerToken(userId, request))
                .isInstanceOf(NotificationException.class)
                .satisfies(exception -> assertThat(((NotificationException) exception).getErrorCode())
                        .isEqualTo(NotificationErrorCode.FCM_TOKEN_REGISTRATION_FAILED));
    }

    @Test
    void unregisterTokenDelegatesToRepositoryDelete() {
        Long userId = 1L;
        String token = "token-to-remove";

        fcmTokenService.unregisterToken(userId, token);

        verify(fcmTokenRepository, times(1)).deleteByUserIdAndToken(userId, token);
        verify(fcmTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void storesLocationWhenProvided() {
        Long userId = 1L;
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO(
                "token-loc", DeviceType.ANDROID, new BigDecimal("37.5665"), new BigDecimal("126.9780"));
        FcmToken existing = FcmToken.register(userId, request.token(), request.deviceType());

        when(fcmTokenRepository.findByToken(request.token())).thenReturn(Optional.of(existing));

        fcmTokenService.registerToken(userId, request);

        assertThat(existing.getLastLatitude()).isEqualByComparingTo("37.5665");
        assertThat(existing.getLastLongitude()).isEqualByComparingTo("126.9780");
        assertThat(existing.getLocatedAt()).isNotNull();
    }

    @Test
    void keepsPreviousLocationWhenRequestHasNone() {
        Long userId = 1L;
        FcmToken existing = FcmToken.register(userId, "token-keep", DeviceType.ANDROID);
        LocalDateTime before = LocalDateTime.of(2026, 9, 1, 10, 0);
        existing.updateLocation(new BigDecimal("37.1"), new BigDecimal("127.1"), before);
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO("token-keep", DeviceType.IOS, null, null);

        when(fcmTokenRepository.findByToken("token-keep")).thenReturn(Optional.of(existing));

        fcmTokenService.registerToken(userId, request);

        assertThat(existing.getLastLatitude()).isEqualByComparingTo("37.1");
        assertThat(existing.getLocatedAt()).isEqualTo(before);
    }
}
