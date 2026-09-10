package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    boolean existsByCode(String code);

    Optional<Badge> findByCode(String code);

    List<Badge> findAllByOrderBySortOrderAsc();

    List<Badge> findByIsActiveTrue();

    long countByIsActiveTrue();
}
