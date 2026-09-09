package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionPhoto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MissionPhotoRepository extends JpaRepository<MissionPhoto, Long> {

    Optional<MissionPhoto> findByAttemptId(Long attemptId);

    boolean existsByAttemptId(Long attemptId);

    // 아카이브 목록 조회 전용 — attempt/mission을 join fetch로 한 번에 가져와
    // 사진마다 N+1이 나지 않게 한다. 최신순(createdAt DESC, id DESC) 키셋 페이징.
    // cursorCreatedAt이 null이면 첫 페이지(전체 최신순).
    @Query("""
            select mp from MissionPhoto mp
            join fetch mp.attempt a
            join fetch a.mission m
            where a.userId = :userId
              and (
                  :cursorCreatedAt is null
                  or mp.createdAt < :cursorCreatedAt
                  or (mp.createdAt = :cursorCreatedAt and mp.id < :cursorId)
              )
            order by mp.createdAt desc, mp.id desc
            """)
    List<MissionPhoto> findArchivePage(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
