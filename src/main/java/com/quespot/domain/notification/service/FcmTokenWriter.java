package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// registerToken()의 트랜잭션과 분리된, 독자적으로 롤백 가능한 삽입 전용 컴포넌트.
// 돌려주는 엔티티는 호출부 트랜잭션에선 detached라, 삽입에 들어가야 할 값(위치 포함)은 전부 여기서 넣는다.
@Component
@RequiredArgsConstructor
public class FcmTokenWriter {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FcmToken saveNewToken(Long userId, RegisterFcmTokenRequestDTO request) {
        FcmToken fcmToken = FcmToken.register(userId, request.token(), request.deviceType());
        if (request.hasLocation()) {
            fcmToken.updateLocation(request.latitude(), request.longitude(), LocalDateTime.now());
        }
        return fcmTokenRepository.saveAndFlush(fcmToken);
    }
}
