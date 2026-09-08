package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @Query("""
            select m.candidate.id
            from Mission m
            where m.candidate.id in :candidateIds
            """)
    List<Long> findPublishedCandidateIds(@Param("candidateIds") List<Long> candidateIds);
}
