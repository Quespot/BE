package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    boolean existsByCode(String code);

    List<Badge> findAllByOrderBySortOrderAsc();
}
