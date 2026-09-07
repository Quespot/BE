package com.quespot.domain.spot.repository;

import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.SpotSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotRepository extends JpaRepository<Spot, Long> {

    Optional<Spot> findBySourceAndSourceContentId(SpotSource source, String sourceContentId);
}
