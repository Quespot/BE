package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
}
