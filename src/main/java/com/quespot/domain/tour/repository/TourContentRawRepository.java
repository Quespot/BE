package com.quespot.domain.tour.repository;

import com.quespot.domain.tour.entity.TourContentRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TourContentRawRepository extends JpaRepository<TourContentRaw, Long> {

    List<TourContentRaw> findByOperationOrderByApiModifiedTimeDesc(String operation);

    // 정제가 필요한 원본 ID만 고른다: content_id별 최신 원본 중 아직 spot이 없거나,
    // spot의 원천 수정 시각·분류 룰셋 버전이 다른 것. 변경 없는 원본은 payload를 읽지 않는다.
    @Query(value = """
            select r.id
            from tour_contents_raw r
            join (
                select content_id, max(api_modified_time) as latest_modified_time
                from tour_contents_raw
                where operation = :operation
                group by content_id
            ) latest
              on latest.content_id = r.content_id
             and latest.latest_modified_time = r.api_modified_time
            left join spots s
              on s.source = 'TOUR_API'
             and s.source_content_id = r.content_id
            where r.operation = :operation
              and (
                  s.id is null
                  or s.source_modified_at is null
                  or s.source_modified_at <> r.api_modified_time
                  or s.category_mapping_version is null
                  or s.category_mapping_version <> :categoryMappingVersion
              )
            order by r.id
            """, nativeQuery = true)
    List<Long> findRefinementTargetIds(
            @Param("operation") String operation,
            @Param("categoryMappingVersion") int categoryMappingVersion
    );

    @Query("select count(distinct r.contentId) from TourContentRaw r where r.operation = :operation")
    long countContents(@Param("operation") String operation);
}
