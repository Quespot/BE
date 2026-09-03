package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
}
