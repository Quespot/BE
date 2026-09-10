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
    // clearAutomatically = true가 없으면, 같은 트랜잭션에서 이 upsert 이전에
    // UserPoint를 이미 읽어 영속성 컨텍스트에 캐시해둔 경우 그 뒤의 findById가
    // (DB를 다시 가지 않고) 캐시된 stale 엔티티를 반환해 balanceAfter가 틀어질
    // 수 있다. PointService.credit()은 항상 upsert -> findById 순서라 지금은
    // 문제가 없지만, 향후 호출 순서가 바뀌어도 안전하도록 명시한다.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_points (user_id, balance, total_earned, total_spent, updated_at)
            VALUES (:userId, :amount, :amount, 0, NOW())
            ON DUPLICATE KEY UPDATE
                balance = balance + :amount,
                total_earned = total_earned + :amount,
                updated_at = NOW()
            """, nativeQuery = true)
    void creditBalance(@Param("userId") Long userId, @Param("amount") int amount);

    // 조회 후 검사가 아니라 조건부 UPDATE로 차감한다(CLAUDE.md). 갱신 0행이면
    // 잔액 부족(행이 없는 신규 사용자 포함) — 호출부가 INSUFFICIENT_POINT로 바꾼다.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE user_points
               SET balance = balance - :amount,
                   total_spent = total_spent + :amount,
                   updated_at = NOW()
             WHERE user_id = :userId AND balance >= :amount
            """, nativeQuery = true)
    int debitBalance(@Param("userId") Long userId, @Param("amount") int amount);
}
