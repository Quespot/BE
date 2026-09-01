package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.dto.res.RegisterFcmTokenResponseDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.DeviceType;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO("token-a", DeviceType.ANDROID);
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
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO(token, DeviceType.IOS);

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
        RegisterFcmTokenRequestDTO request = new RegisterFcmTokenRequestDTO("token-race", DeviceType.WEB);
        FcmToken raceWinnerToken = FcmToken.register(userId, request.token(), request.deviceType());

        when(fcmTokenRepository.findByToken(request.token()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(raceWinnerToken));
        when(fcmTokenWriter.saveNewToken(userId, request))
                .thenThrow(new DataIntegrityViolationException("duplicate token"));

        RegisterFcmTokenResponseDTO response = fcmTokenService.registerToken(userId, request);

        assertThat(response.deviceType()).isEqualTo(DeviceType.WEB);
        assertThat(raceWinnerToken.getUserId()).isEqualTo(userId);
    }

    @Test
    void unregisterTokenDelegatesToRepositoryDelete() {
        Long userId = 1L;
        String token = "token-to-remove";

        fcmTokenService.unregisterToken(userId, token);

        verify(fcmTokenRepository, times(1)).deleteByUserIdAndToken(userId, token);
        verify(fcmTokenRepository, never()).findByToken(anyString());
    }
}
