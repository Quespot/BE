package com.quespot.domain.tour.repository;

import com.quespot.domain.tour.entity.ApiQuotaUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ApiQuotaUsageRepository extends JpaRepository<ApiQuotaUsage, LocalDate> {

    @Modifying
    @Query(value = "INSERT IGNORE INTO api_quota_usages (usage_date, call_count) VALUES (:date, 0)", nativeQuery = true)
    void insertIfAbsent(@Param("date") LocalDate date);

    @Modifying
    @Query(value = """
            UPDATE api_quota_usages
               SET call_count = call_count + 1
             WHERE usage_date = :date
               AND call_count < :dailyLimit
            """, nativeQuery = true)
    int tryIncrement(@Param("date") LocalDate date, @Param("dailyLimit") int dailyLimit);
}
