package com.quespot.domain.tour.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RawPersister {

    private final RawContentRowWriter rawContentRowWriter;

    public RawPersister(RawContentRowWriter rawContentRowWriter) {
        this.rawContentRowWriter = rawContentRowWriter;
    }

    // 건별로 별도 물리 트랜잭션(RawContentRowWriter.saveOne)에 저장한다 — 한
    // 건이 실패해도 그 트랜잭션만 롤백되고 나머지 건의 트랜잭션에는 영향이 없다.
    public void saveAll(String operation, List<JsonNode> items) {
        for (JsonNode node : items) {
            try {
                rawContentRowWriter.saveOne(operation, node);
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate tour content skipped: operation={}, node={}", operation, node, e);
            } catch (JsonProcessingException e) {
                log.error("Malformed tour content item skipped: operation={}, node={}", operation, node, e);
            } catch (RuntimeException e) {
                // modifiedtime이 비어있거나("") 형식이 깨진 경우(DateTimeParseException) 등.
                log.error("Unexpected error while saving tour content item, skipped: operation={}, node={}", operation, node, e);
            }
        }
    }
}
