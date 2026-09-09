package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.CourseMission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseMissionRepository extends JpaRepository<CourseMission, Long> {

    @EntityGraph(attributePaths = "mission")
    List<CourseMission> findByCourseIdOrderBySeq(Long courseId);

    @Query("select cm.mission.id from CourseMission cm where cm.course.id = :courseId")
    List<Long> findMissionIdsByCourseId(@Param("courseId") Long courseId);

    boolean existsByCourseIdAndMissionId(Long courseId, Long missionId);
}
