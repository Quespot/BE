package com.quespot.domain.notification.service;

import com.quespot.domain.notification.converter.NotificationConverter;
import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.dto.res.RegisterFcmTokenResponseDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    // 토큰 등록: 이미 등록된 토큰이면 소유자를 갱신한다 (기기 재할당 대응)
    @Transactional
    public RegisterFcmTokenResponseDTO registerToken(Long userId, RegisterFcmTokenRequestDTO request) {
        FcmToken fcmToken = fcmTokenRepository.findByToken(request.token())
                .orElseGet(() -> saveNewToken(userId, request));

        fcmToken.reassignTo(userId, request.deviceType());

        return NotificationConverter.toRegisterFcmTokenResponseDTO(fcmToken);
    }

    @Transactional
    public void unregisterToken(Long userId, String token) {
        fcmTokenRepository.deleteByUserIdAndToken(userId, token);
    }

    private FcmToken saveNewToken(Long userId, RegisterFcmTokenRequestDTO request) {
        try {
            return fcmTokenRepository.saveAndFlush(
                    FcmToken.register(userId, request.token(), request.deviceType())
            );
        } catch (DataIntegrityViolationException exception) {
            return fcmTokenRepository.findByToken(request.token())
                    .orElseThrow(() -> exception);
        }
    }
}
