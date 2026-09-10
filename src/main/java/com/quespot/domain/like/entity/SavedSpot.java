package com.quespot.domain.like.entity;

import com.quespot.domain.spot.entity.Spot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 좋아요와 별개 기능. saved_at이 목록 정렬 기준이다.
// 등록은 SavedSpotRepository.upsert(네이티브)로만 한다.
@Entity
@Table(
        name = "saved_spots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_saved_spot", columnNames = {"user_id", "spot_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;
}
