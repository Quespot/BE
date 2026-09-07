package com.quespot.domain.tour.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.tour.client.dto.TourSyncItem;
import com.quespot.domain.tour.entity.TourContentRaw;
import com.quespot.domain.tour.repository.TourContentRawRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class RawPersister {

    private static final DateTimeFormatter MODIFIED_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TourContentRawRepository tourContentRawRepository;
    private final ObjectMapper objectMapper;

    public RawPersister(TourContentRawRepository tourContentRawRepository, ObjectMapper objectMapper) {
        this.tourContentRawRepository = tourContentRawRepository;
        this.objectMapper = objectMapper;
    }

    // 페이지 단위 커밋 — 20페이지에서 죽어도 앞 19페이지는 남아야 한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAll(String operation, List<JsonNode> items) {
        for (JsonNode node : items) {
            try {
                save(operation, node);
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate tour content skipped: operation={}, node={}", operation, node, e);
            } catch (JsonProcessingException e) {
                log.error("Malformed tour content item skipped: operation={}, node={}", operation, node, e);
            } catch (RuntimeException e) {
                // modifiedtime이 비어있거나("") 형식이 깨진 경우(DateTimeParseException) 등.
                // 여기서 안 잡으면 REQUIRES_NEW 트랜잭션 전체가 롤백돼 이미 flush한
                // 앞 아이템들까지 같이 사라지고, 체크포인트는 advance() 없이 fail()만
                // 찍혀 같은 페이지에서 영원히 막힌다.
                log.error("Unexpected error while saving tour content item, skipped: operation={}, node={}", operation, node, e);
            }
        }
    }

    private void save(String operation, JsonNode node) throws JsonProcessingException {
        TourSyncItem item = objectMapper.treeToValue(node, TourSyncItem.class);

        TourContentRaw raw = TourContentRaw.create(
                item.contentid(),
                operation,
                node.toString(),
                LocalDateTime.parse(item.modifiedtime(), MODIFIED_TIME_FORMAT),
                "1".equals(item.showflag())
        );

        // 건별로 즉시 flush해야 한다. save()만 반복하면 Hibernate가 커밋 시점에
        // 한꺼번에 flush해서, 한 건이 UNIQUE 위반이어도 최종 flush에서 배치
        // 전체가 같이 죽는다.
        tourContentRawRepository.saveAndFlush(raw);
    }
}
