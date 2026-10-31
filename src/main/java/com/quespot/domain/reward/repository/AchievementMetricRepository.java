package com.quespot.domain.reward.repository;

import com.quespot.domain.mission.entity.MissionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 배지 판정용 집계 쿼리 모음. 엔티티 타입은 형식상 MissionAttempt지만 저장 용도가
// 아니라 count 전용이다. 인덱스: mission_attempts(user_id, status), missions PK,
// spots PK, mission_photos.attempt_id UNIQUE, archive_photos(user_id, created_at),
// course_attempts(user_id, status) — 새 인덱스 불필요(#50 스펙 2.2).
public interface AchievementMetricRepository extends JpaRepository<MissionAttempt, Long> {

    // (user, mission)당 COMPLETED는 최대 1건이라 시도 수 = 완료 미션 수.
    @Query("""
            select count(ma)
            from MissionAttempt ma
              join ma.mission m
              join m.spot s
            where ma.userId = :userId
              and ma.status = com.quespot.domain.mission.enums.MissionAttemptStatus.COMPLETED
              and (:regionCode is null or s.ldongRegnCd = :regionCode)
            """)
    long countCompletedMissions(@Param("userId") Long userId, @Param("regionCode") String regionCode);

    @Query("""
            select count(distinct s.ldongSignguCd)
            from MissionAttempt ma
              join ma.mission m
              join m.spot s
            where ma.userId = :userId
              and ma.status = com.quespot.domain.mission.enums.MissionAttemptStatus.COMPLETED
              and s.ldongRegnCd = :regionCode
              and s.ldongSignguCd is not null
            """)
    long countDistinctDistricts(@Param("userId") Long userId, @Param("regionCode") String regionCode);

    @Query("select count(mp) from MissionPhoto mp join mp.attempt ma where ma.userId = :userId")
    long countMissionPhotos(@Param("userId") Long userId);

    @Query("select count(ap) from ArchivePhoto ap where ap.userId = :userId")
    long countArchivePhotos(@Param("userId") Long userId);

    @Query("""
            select count(ca) from CourseAttempt ca
            where ca.userId = :userId
              and ca.status = com.quespot.domain.mission.enums.CourseAttemptStatus.COMPLETED
            """)
    long countCompletedCourses(@Param("userId") Long userId);
}
