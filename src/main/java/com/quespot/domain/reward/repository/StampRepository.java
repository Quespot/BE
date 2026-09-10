package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.Stamp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StampRepository extends JpaRepository<Stamp, Long> {

    boolean existsByCode(String code);

    List<Stamp> findAllByOrderBySortOrderAsc();

    Optional<Stamp> findByRegionCode(String regionCode);
}
