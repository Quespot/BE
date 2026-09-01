package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// registerToken()의 트랜잭션과 분리된, 독자적으로 롤백 가능한 삽입 전용 컴포넌트
@Component
@RequiredArgsConstructor
public class FcmTokenWriter {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FcmToken saveNewToken(Long userId, RegisterFcmTokenRequestDTO request) {
        return fcmTokenRepository.saveAndFlush(
                FcmToken.register(userId, request.token(), request.deviceType())
        );
    }
}
