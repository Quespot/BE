package com.quespot.domain.tour.repository;

import com.quespot.domain.tour.entity.TourContentRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourContentRawRepository extends JpaRepository<TourContentRaw, Long> {

    List<TourContentRaw> findByOperationOrderByApiModifiedTimeDesc(String operation);
}
