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

    long countByUserId(Long userId);
}
