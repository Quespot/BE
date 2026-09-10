package com.quespot.domain.like.repository;

import com.quespot.domain.like.entity.Like;
import com.quespot.domain.like.enums.LikeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeRepository extends JpaRepository<Like, Long> {

    // 이미 좋아요 상태에서 다시 POST가 와도 예외 없이 no-op — UNIQUE 위반을
    // 같은 트랜잭션에서 catch하면 rollback-only가 되므로(CLAUDE.md) 예외 경로
    // 자체를 없앤다. INSERT IGNORE는 절단 같은 다른 오류까지 삼켜서 쓰지 않는다.
    @Modifying
    @Query(value = """
            INSERT INTO likes (user_id, target_type, target_id, created_at)
            VALUES (:userId, :targetType, :targetId, NOW())
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void upsert(@Param("userId") Long userId,
                @Param("targetType") String targetType,
                @Param("targetId") Long targetId);

    long deleteByUserIdAndTargetTypeAndTargetId(Long userId, LikeTargetType targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, LikeTargetType targetType, Long targetId);
}
