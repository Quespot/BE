package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionCourseRepository extends JpaRepository<MissionCourse, Long> {

    List<MissionCourse> findByStatus(MissionCourseStatus status);

    List<MissionCourse> findByRegionCodeAndStatus(String regionCode, MissionCourseStatus status);

    Optional<MissionCourse> findByIdAndStatus(Long id, MissionCourseStatus status);
}
