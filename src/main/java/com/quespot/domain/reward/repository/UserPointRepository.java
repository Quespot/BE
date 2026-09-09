package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

    // 가입 시 user_points를 초기화하는 훅이 레포 전체에 없다. 최초 지급과
    // 누적 지급을 한 문장으로 원자 처리해 "행이 없으면 만들고, 있으면
    // 더한다"를 동시성 문제 없이 처리한다.
    @Modifying
    @Query(value = """
            INSERT INTO user_points (user_id, balance, total_earned, total_spent, updated_at)
            VALUES (:userId, :amount, :amount, 0, NOW())
            ON DUPLICATE KEY UPDATE
                balance = balance + :amount,
                total_earned = total_earned + :amount,
                updated_at = NOW()
            """, nativeQuery = true)
    void creditBalance(@Param("userId") Long userId, @Param("amount") int amount);
}
