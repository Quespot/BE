package com.quespot.domain.notification.repository;

import com.quespot.domain.notification.dto.RecommendableMissionDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// missions/mission_attempts는 허건우 담당 엔티티라 MissionRepository에 메서드를 넣지 않고
// 알림 도메인에서 네이티브 SQL로 읽기만 한다. 미션이 수십 건이라 rand() 정렬로 충분하다.
@Repository
public class RecommendableMissionRepository {

    private static final String SQL = """
            select m.id, m.title, m.snapshot_name, m.reward_point
              from missions m
             where m.status = 'ACTIVE'
               and ST_Distance_Sphere(point(m.snapshot_longitude, m.snapshot_latitude), point(:lng, :lat)) <= :radius
               and m.id not in (
                   select a.mission_id from mission_attempts a
                    where a.user_id = :userId and a.status in ('IN_PROGRESS', 'COMPLETED')
               )
             order by rand()
             limit 1
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Optional<RecommendableMissionDTO> pickRandomNearby(Long userId, BigDecimal latitude, BigDecimal longitude,
                                                              int radiusMeters) {
        List<Object[]> rows = entityManager.createNativeQuery(SQL)
                .setParameter("userId", userId)
                .setParameter("lat", latitude)
                .setParameter("lng", longitude)
                .setParameter("radius", radiusMeters)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new RecommendableMissionDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).intValue()
        ));
    }
}
