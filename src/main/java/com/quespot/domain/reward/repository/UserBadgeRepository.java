package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserId(Long userId);

    @Query("select ub.badge.id from UserBadge ub where ub.userId = :userId")
    List<Long> findBadgeIdsByUserId(@Param("userId") Long userId);

    // 달성 현황의 분자 — 분모(countByIsActiveTrue)와 같은 활성 기준. 비활성화된 배지를
    // 이미 획득한 사용자가 있어도 "획득 6 / 전체 5"가 되지 않게.
    long countByUserIdAndBadge_IsActiveTrue(Long userId);
}
