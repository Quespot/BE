package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.RewardActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RewardActivityRepository extends JpaRepository<RewardActivity, Long> {

    @Query("""
            SELECT ra FROM RewardActivity ra
            LEFT JOIN FETCH ra.pointTransaction pt
            WHERE ra.userId = :userId
              AND (:cursor IS NULL OR ra.id < :cursor)
            ORDER BY ra.id DESC
            """)
    List<RewardActivity> findFeed(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);
}
