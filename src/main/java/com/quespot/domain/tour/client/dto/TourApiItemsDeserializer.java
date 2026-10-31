package com.quespot.domain.tour.client.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.util.List;

// TourAPI는 items에 결과가 없으면 객체가 아니라 빈 문자열("")을 내려준다.
// 정상 케이스는 {"item": [...]} 또는 결과 1건일 때 {"item": {...}}(후자는
// ACCEPT_SINGLE_VALUE_AS_ARRAY 설정이 배열로 승격시켜준다).
public class TourApiItemsDeserializer extends JsonDeserializer<List<Object>> implements ContextualDeserializer {

    private JavaType contentType;

    @Override
    public List<Object> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = ctxt.readTree(parser);

        if (node.isTextual()) {
            return List.of();
        }

        JsonNode itemNode = node.path("item");
        JavaType listType = ctxt.getTypeFactory().constructCollectionType(List.class, contentType);

        return ctxt.readTreeAsValue(itemNode, listType);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        TourApiItemsDeserializer deserializer = new TourApiItemsDeserializer();
        deserializer.contentType = property.getType().getContentType();
        return deserializer;
    }
}
