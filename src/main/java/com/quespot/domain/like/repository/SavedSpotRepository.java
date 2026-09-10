package com.quespot.domain.like.repository;

import com.quespot.domain.like.entity.SavedSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedSpotRepository extends JpaRepository<SavedSpot, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO saved_spots (user_id, spot_id, saved_at)
            VALUES (:userId, :spotId, NOW())
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("spotId") Long spotId);

    long deleteByUserIdAndSpot_Id(Long userId, Long spotId);

    boolean existsByUserIdAndSpot_Id(Long userId, Long spotId);
}
