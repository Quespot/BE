package com.quespot.domain.notification.repository;

import com.quespot.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    void deleteByUserIdAndToken(Long userId, String token);

    List<FcmToken> findAllByUserId(Long userId);

    void deleteByTokenIn(Collection<String> tokens);

    // 스케줄러 대상: 푸시 켠 사용자(notification_settings 행이 있고 true)의, 유효기간 안 위치가 있는 토큰.
    // 행이 없는 사용자는 허건우 코드(NotificationSettingService) 기준으로 푸시 off다.
    @Query("""
            select t from FcmToken t
             where t.locatedAt >= :since
               and t.userId in (select s.userId from NotificationSetting s where s.pushEnabled = true)
            """)
    List<FcmToken> findLocatedTokensOfPushEnabledUsers(@Param("since") LocalDateTime since);
}
