package com.quespot.domain.spot.repository;

import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpotRepository extends JpaRepository<Spot, Long> {

    Optional<Spot> findBySourceAndSourceContentId(SpotSource source, String sourceContentId);

    @Query("""
            select s
            from Spot s
            where s.source = :source
              and s.showFlag = true
              and s.ldongRegnCd = :regionCode
              and s.appCategory in :categories
              and (s.imageUrl is not null or s.thumbnailUrl is not null)
            order by s.id
            """)
    List<Spot> findMissionCandidateEligibleSpots(
            @Param("source") SpotSource source,
            @Param("regionCode") String regionCode,
            @Param("categories") List<AppCategory> categories
    );
}
