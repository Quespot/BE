package com.quespot.domain.like.repository;

import com.quespot.domain.like.dto.LikedCourseRowDTO;
import com.quespot.domain.like.dto.LikedMissionRowDTO;
import com.quespot.domain.like.entity.Like;
import com.quespot.domain.like.enums.LikeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    // likes엔 FK가 없어 연관관계 대신 `join ... on`으로 붙인다. 비활성 미션은
    // 목록에서 빠진다(좋아요 행은 남겨둔다 — 해제는 사용자가 한다).
    @Query("""
            select new com.quespot.domain.like.dto.LikedMissionRowDTO(m, l.createdAt)
            from UserLike l
              join Mission m on m.id = l.targetId
            where l.userId = :userId
              and l.targetType = com.quespot.domain.like.enums.LikeTargetType.MISSION
              and m.status = com.quespot.domain.mission.enums.MissionStatus.ACTIVE
            order by l.createdAt desc, l.id desc
            """)
    List<LikedMissionRowDTO> findLikedMissions(@Param("userId") Long userId);

    @Query("""
            select new com.quespot.domain.like.dto.LikedCourseRowDTO(c, l.createdAt)
            from UserLike l
              join MissionCourse c on c.id = l.targetId
            where l.userId = :userId
              and l.targetType = com.quespot.domain.like.enums.LikeTargetType.COURSE
              and c.status = com.quespot.domain.mission.enums.MissionCourseStatus.ACTIVE
            order by l.createdAt desc, l.id desc
            """)
    List<LikedCourseRowDTO> findLikedCourses(@Param("userId") Long userId);
}
