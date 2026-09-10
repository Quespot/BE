package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.UserStamp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStampRepository extends JpaRepository<UserStamp, Long> {

    List<UserStamp> findByUserId(Long userId);

    boolean existsByUserIdAndStamp_Id(Long userId, Long stampId);

    long countByUserId(Long userId);
}
