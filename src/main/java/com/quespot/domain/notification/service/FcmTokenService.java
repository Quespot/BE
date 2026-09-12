package com.quespot.domain.notification.service;

import com.quespot.domain.notification.converter.NotificationConverter;
import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.dto.res.RegisterFcmTokenResponseDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.exception.NotificationException;
import com.quespot.domain.notification.exception.code.NotificationErrorCode;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmTokenWriter fcmTokenWriter;

    // 토큰 등록: 이미 등록된 토큰이면 소유자를 갱신한다 (기기 재할당 대응).
    // 새 토큰은 FcmTokenWriter가 REQUIRES_NEW로 넣고 돌려주는데, 그 엔티티는 이 트랜잭션의
    // 영속성 컨텍스트에 없는(detached) 객체라 여기서 고쳐도 flush되지 않는다 — 그래서 위치는
    // 삽입 시점에 writer가 같이 넣고, 이 메서드는 "이미 관리 중인" 엔티티만 갱신한다(#57 리뷰).
    @Transactional
    public RegisterFcmTokenResponseDTO registerToken(Long userId, RegisterFcmTokenRequestDTO request) {
        FcmToken fcmToken = fcmTokenRepository.findByToken(request.token())
                .map(existing -> refresh(existing, userId, request))
                .orElseGet(() -> saveNewToken(userId, request));

        return NotificationConverter.toRegisterFcmTokenResponseDTO(fcmToken);
    }

    @Transactional
    public void unregisterToken(Long userId, String token) {
        fcmTokenRepository.deleteByUserIdAndToken(userId, token);
    }

    private FcmToken saveNewToken(Long userId, RegisterFcmTokenRequestDTO request) {
        try {
            return fcmTokenWriter.saveNewToken(userId, request);
        } catch (DataIntegrityViolationException exception) {
            // 따닥 요청에서 진 쪽: 이긴 쪽이 넣은 행을 이 컨텍스트로 읽어(관리 상태) 갱신한다.
            return fcmTokenRepository.findByToken(request.token())
                    .map(existing -> refresh(existing, userId, request))
                    .orElseThrow(() -> new NotificationException(
                            NotificationErrorCode.FCM_TOKEN_REGISTRATION_FAILED
                    ));
        }
    }

    private FcmToken refresh(FcmToken managed, Long userId, RegisterFcmTokenRequestDTO request) {
        managed.reassignTo(userId, request.deviceType());
        if (request.hasLocation()) {
            managed.updateLocation(request.latitude(), request.longitude(), LocalDateTime.now());
        }
        return managed;
    }
}
