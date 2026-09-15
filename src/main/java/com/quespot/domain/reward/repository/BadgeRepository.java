package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    boolean existsByCode(String code);

    Optional<Badge> findByCode(String code);

    // 목록 노출용 — 분모(countByIsActiveTrue)·판정(findByIsActiveTrue)과 같은 활성 기준.
    // 폐기·보류 배지는 user_badges FK 때문에 행이 남아 있어서 findAll이면 같이 딸려온다(#64).
    List<Badge> findByIsActiveTrueOrderBySortOrderAsc();

    List<Badge> findByIsActiveTrue();

    long countByIsActiveTrue();
}
