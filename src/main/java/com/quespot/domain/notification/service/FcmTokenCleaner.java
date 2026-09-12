package com.quespot.domain.notification.service;

import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// AFTER_COMMIT 리스너에서 호출된다. 그 시점엔 원래 트랜잭션이 이미 커밋돼 있어 기본 전파(REQUIRED)로
// JPA 쓰기를 하면 커밋된 트랜잭션에 "합류"만 하고 실제 반영이 안 된다. 그래서 별도 Bean의
// REQUIRES_NEW로 새 트랜잭션을 연다(self-invocation은 프록시를 안 타므로 반드시 다른 Bean).
@Component
@RequiredArgsConstructor
public class FcmTokenCleaner {

    private final FcmTokenRepository fcmTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteByTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        fcmTokenRepository.deleteByTokenIn(tokens);
    }
}
