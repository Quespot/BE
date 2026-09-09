package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionPhotoRepository extends JpaRepository<MissionPhoto, Long> {

    Optional<MissionPhoto> findByAttemptId(Long attemptId);

    boolean existsByAttemptId(Long attemptId);
}
