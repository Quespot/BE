package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.req.RegisterFcmTokenRequestDTO;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

// registerToken()의 트랜잭션과 분리된, 독자적으로 커밋/롤백되는 쓰기 전용 컴포넌트.
// 돌려주는 엔티티는 호출부 트랜잭션에선 detached라, 저장돼야 할 값은 전부 여기서 넣고 커밋한다.
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

    // 따닥 등록에서 진 쪽이 쓴다. registerToken()의 트랜잭션은 첫 findByToken()에서 REPEATABLE READ
    // 스냅샷을 잡아, 그 뒤 이긴 쪽이 커밋한 행을 같은 트랜잭션의 재조회로는 볼 수 없다.
    // 그래서 재조회와 갱신을 새 트랜잭션(새 스냅샷)에서 한다(PR #58 CodeRabbit 지적).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<FcmToken> reassignExisting(Long userId, RegisterFcmTokenRequestDTO request) {
        return fcmTokenRepository.findByToken(request.token())
                .map(existing -> apply(existing, userId, request));
    }

    // 이미 있는 토큰에 등록 요청을 반영한다. 관리 상태(managed) 엔티티에만 쓴다.
    // 소유자가 바뀌었는데 새 위치가 없으면 이전 소유자의 좌표를 지운다.
    static FcmToken apply(FcmToken managed, Long userId, RegisterFcmTokenRequestDTO request) {
        boolean ownerChanged = !userId.equals(managed.getUserId());
        managed.reassignTo(userId, request.deviceType());
        if (request.hasLocation()) {
            managed.updateLocation(request.latitude(), request.longitude(), LocalDateTime.now());
        } else if (ownerChanged) {
            managed.clearLocation();
        }
        return managed;
    }
}
