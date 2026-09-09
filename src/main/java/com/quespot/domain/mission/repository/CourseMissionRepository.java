package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.repository.projection.CourseMissionLockRowProjection;
import com.quespot.domain.mission.repository.projection.ExistingCoursePairProjection;
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

    // 코스 생성 알고리즘의 페어링 재시도용 — 같은 사용자가 같은 anchor로 이미 만든
    // 코스들의 2·3번 미션 조합을 한 번에 가져온다(#39 재설계).
    @Query(value = """
            select cm2.mission_id as mission2Id, cm3.mission_id as mission3Id
            from mission_courses c
            join course_missions cm2 on cm2.course_id = c.id and cm2.seq = 2
            join course_missions cm3 on cm3.course_id = c.id and cm3.seq = 3
            where c.created_by_user_id = :userId and c.anchor_mission_id = :anchorMissionId
            """, nativeQuery = true)
    List<ExistingCoursePairProjection> findExistingPairs(
            @Param("userId") Long userId, @Param("anchorMissionId") Long anchorMissionId
    );

    // CourseLockPolicy.resolveLockedMissionIds 전용 — 이 유저의 IN_PROGRESS 코스들
    // 중 주어진 missionIds가 속한 행만, 같은 코스의 바로 이전 seq 미션ID와 함께
    // 가져온다(seq=1이면 prevMissionId는 null). #39 재설계.
    @Query(value = """
            select cm.mission_id as missionId, prev.mission_id as prevMissionId
            from course_missions cm
            join course_attempts ca on ca.course_id = cm.course_id
                and ca.user_id = :userId and ca.status = 'IN_PROGRESS'
            left join course_missions prev on prev.course_id = cm.course_id and prev.seq = cm.seq - 1
            where cm.mission_id in (:missionIds)
            """, nativeQuery = true)
    List<CourseMissionLockRowProjection> findLockRowsForInProgressCourses(
            @Param("userId") Long userId, @Param("missionIds") List<Long> missionIds
    );
}
