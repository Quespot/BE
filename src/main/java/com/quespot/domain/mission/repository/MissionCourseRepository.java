package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionCourseRepository extends JpaRepository<MissionCourse, Long> {

    List<MissionCourse> findByStatus(MissionCourseStatus status);

    List<MissionCourse> findByRegionCodeAndStatus(String regionCode, MissionCourseStatus status);
}
