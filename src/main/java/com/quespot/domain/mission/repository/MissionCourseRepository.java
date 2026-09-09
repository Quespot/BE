package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionCourseRepository extends JpaRepository<MissionCourse, Long> {

    Optional<MissionCourse> findByIdAndStatus(Long id, MissionCourseStatus status);

    List<MissionCourse> findByCreatedByUserIdOrderByCreatedAtDesc(Long createdByUserId);
}
