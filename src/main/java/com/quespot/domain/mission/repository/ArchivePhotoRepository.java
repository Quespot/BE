package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.repository.projection.ArchiveFeedRowProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArchivePhotoRepository extends JpaRepository<ArchivePhoto, Long> {

    Optional<ArchivePhoto> findByUserIdAndImageKey(Long userId, String imageKey);

    // mission_photos(미션 완료 사진) + archive_photos(자유 업로드 사진)를 하나의
    // 최신순 피드로 합친다(#45 확장). 두 테이블의 id가 서로 겹칠 수 있어 정렬·
    // 커서 비교 모두 (createdAt, source, id) 세 값을 키로 쓴다 — source는
    // 오름차순(ARCHIVE < MISSION)으로 고정해서 정렬 순서와 커서 비교 조건이
    // 항상 일치하게 한다. mission 쪽은 JOIN으로 이미 필요한 필드를 다 채워서
    // 내려주므로 이후 지연 로딩으로 인한 N+1이 없다.
    @Query(value = """
            select * from (
                select
                    mp.id as id, 'MISSION' as source, mp.image_url as imageKey, mp.caption as caption,
                    m.id as missionId, m.title as missionTitle, m.category as missionCategory,
                    ma.completed_at as completedAt, mp.created_at as createdAt
                from mission_photos mp
                join mission_attempts ma on ma.id = mp.attempt_id
                join missions m on m.id = ma.mission_id
                where ma.user_id = :userId
                union all
                select
                    ap.id as id, 'ARCHIVE' as source, ap.image_key as imageKey, ap.caption as caption,
                    null as missionId, null as missionTitle, null as missionCategory,
                    null as completedAt, ap.created_at as createdAt
                from archive_photos ap
                where ap.user_id = :userId
            ) combined
            where :cursorCreatedAt is null
               or combined.createdAt < :cursorCreatedAt
               or (combined.createdAt = :cursorCreatedAt and combined.source > :cursorSource)
               or (
                   combined.createdAt = :cursorCreatedAt and combined.source = :cursorSource
                   and combined.id < :cursorId
               )
            order by combined.createdAt desc, combined.source asc, combined.id desc
            limit :limit
            """, nativeQuery = true)
    List<ArchiveFeedRowProjection> findFeedPage(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorSource") String cursorSource,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );
}
