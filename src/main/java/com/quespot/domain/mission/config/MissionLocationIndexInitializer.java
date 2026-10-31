package com.quespot.domain.mission.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// missions에 위치 생성 컬럼(snapshot_location)과 공간 인덱스(R-tree)를 만든다.
// JPA의 @Index는 SPATIAL 인덱스와 생성 컬럼을 표현할 수 없어서, Hibernate가 스키마를 만든 뒤
// (@DependsOn("entityManagerFactory")) 없을 때만 DDL을 실행한다. 웹 요청을 받기 전에 끝나도록 @PostConstruct를 쓴다.
// SRID 0(평면 좌표)인 POINT(경도, 위도)라서 위경도 bounding box를 그대로 MBRContains 조건으로 쓸 수 있다.
@Slf4j
@Component
@DependsOn("entityManagerFactory")
@RequiredArgsConstructor
public class MissionLocationIndexInitializer {

    static final String LOCATION_COLUMN = "snapshot_location";
    static final String LOCATION_INDEX = "sx_missions_location";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void createLocationColumnAndIndex() {
        if (!columnExists()) {
            jdbcTemplate.execute("""
                    ALTER TABLE missions
                    ADD COLUMN snapshot_location POINT
                        AS (POINT(snapshot_longitude, snapshot_latitude)) STORED SRID 0 NOT NULL
                    """);
            log.info("missions.{} 생성 컬럼을 추가했습니다.", LOCATION_COLUMN);
        }
        if (!indexExists()) {
            jdbcTemplate.execute("CREATE SPATIAL INDEX sx_missions_location ON missions (snapshot_location)");
            log.info("missions.{} 공간 인덱스를 추가했습니다.", LOCATION_INDEX);
        }
    }

    private boolean columnExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name = 'missions'
                   AND column_name = ?
                """, Integer.class, LOCATION_COLUMN);
        return count != null && count > 0;
    }

    private boolean indexExists() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'missions'
                   AND index_name = ?
                """, Integer.class, LOCATION_INDEX);
        return count != null && count > 0;
    }
}
