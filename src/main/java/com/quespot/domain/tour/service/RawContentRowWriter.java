package com.quespot.domain.tour.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.tour.client.dto.TourSyncItem;
import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// RawPersister.saveAll()의 반복문 밖으로 건별 저장을 분리한 이유는 트랜잭션
// 경계 때문이다. saveAll이 스스로 REQUIRES_NEW였을 때는 그 안의 여러 건이
// 물리적으로 같은 새 트랜잭션을 공유했다 — 한 건에서 saveAndFlush()가
// DataIntegrityViolationException을 던지면 애플리케이션 코드가 catch해도
// Hibernate/Spring이 그 트랜잭션을 이미 rollback-only로 마킹해버려서, 이후
// 정상 건까지 포함해 그 트랜잭션 전체가 UnexpectedRollbackException으로
// 커밋에 실패한다(2026-09-07 실DB 스파이크 테스트로 실증).
//
// saveOne()을 별도 빈의 @Transactional(REQUIRES_NEW)로 두면 Spring AOP
// 프록시를 거쳐 매 호출마다 완전히 새로운 물리 트랜잭션이 생긴다. 한 건이
// 실패해 그 트랜잭션이 rollback-only가 되어도 그건 그 트랜잭션 자체이지
// 다음 건의 트랜잭션이 아니므로 서로 전염되지 않는다.
@Component
public class RawContentRowWriter {

    private static final DateTimeFormatter MODIFIED_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TourContentRawRepository tourContentRawRepository;
    private final ObjectMapper objectMapper;

    public RawContentRowWriter(TourContentRawRepository tourContentRawRepository, ObjectMapper objectMapper) {
        this.tourContentRawRepository = tourContentRawRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOne(String operation, JsonNode node) throws JsonProcessingException {
        TourSyncItem item = objectMapper.treeToValue(node, TourSyncItem.class);

        TourContentRaw raw = TourContentRaw.create(
                item.contentid(),
                operation,
                node.toString(),
                LocalDateTime.parse(item.modifiedtime(), MODIFIED_TIME_FORMAT),
                "1".equals(item.showflag())
        );

        tourContentRawRepository.saveAndFlush(raw);
    }
}
