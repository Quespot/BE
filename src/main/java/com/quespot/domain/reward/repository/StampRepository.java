package com.quespot.domain.reward.repository;

import com.quespot.domain.reward.entity.Stamp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StampRepository extends JpaRepository<Stamp, Long> {

    boolean existsByCode(String code);

    List<Stamp> findAllByOrderBySortOrderAsc();
}
