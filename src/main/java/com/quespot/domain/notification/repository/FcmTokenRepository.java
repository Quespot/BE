package com.quespot.domain.notification.repository;

import com.quespot.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    void deleteByUserIdAndToken(Long userId, String token);

    List<FcmToken> findAllByUserId(Long userId);

    void deleteByTokenIn(Collection<String> tokens);
}
