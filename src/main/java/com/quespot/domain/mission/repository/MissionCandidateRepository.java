package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.dto.MissionCandidateGenerationKeyDTO;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionCandidateStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MissionCandidateRepository extends JpaRepository<MissionCandidate, Long> {

    @Query("""
            select new com.quespot.domain.mission.dto.MissionCandidateGenerationKeyDTO(
                c.spot.id,
                c.templateCode
            )
            from MissionCandidate c
            where c.generatorVersion = :generatorVersion
            """)
    List<MissionCandidateGenerationKeyDTO> findGenerationKeys(
            @Param("generatorVersion") Integer generatorVersion
    );

    @EntityGraph(attributePaths = {"spot", "reviewedBy"})
    Page<MissionCandidate> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"spot", "reviewedBy"})
    Page<MissionCandidate> findAllByStatus(MissionCandidateStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"spot", "reviewedBy"})
    Page<MissionCandidate> findAllBySuggestedCategory(MissionCategory category, Pageable pageable);

    @EntityGraph(attributePaths = {"spot", "reviewedBy"})
    Page<MissionCandidate> findAllByStatusAndSuggestedCategory(
            MissionCandidateStatus status,
            MissionCategory category,
            Pageable pageable
    );

    @Query("""
            select c
            from MissionCandidate c
            join fetch c.spot
            left join fetch c.reviewedBy
            where c.id = :candidateId
            """)
    Optional<MissionCandidate> findDetailById(@Param("candidateId") Long candidateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MissionCandidate c where c.id = :candidateId")
    Optional<MissionCandidate> findByIdForUpdate(@Param("candidateId") Long candidateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from MissionCandidate c
            where c.status = :status
              and c.suggestedCategory = :category
            order by c.id
            """)
    List<MissionCandidate> findAllByStatusAndCategoryForUpdate(
            @Param("status") MissionCandidateStatus status,
            @Param("category") MissionCategory category
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from MissionCandidate c
            join fetch c.spot
            where c.status = :status
              and c.suggestedCategory = :category
            order by c.id
            """)
    List<MissionCandidate> findAllByStatusAndCategoryWithSpotForUpdate(
            @Param("status") MissionCandidateStatus status,
            @Param("category") MissionCategory category
    );
}
