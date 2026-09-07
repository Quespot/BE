package com.quespot.domain.tour.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "api_quota_usages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiQuotaUsage {

    @Id
    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Column(name = "call_count", nullable = false)
    private Integer callCount;
}
