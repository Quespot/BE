package com.quespot.domain.tour.client.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    @Test
    void emptyStringItemsDeserializeToEmptyList() throws Exception {
        String json = """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":"","numOfRows":10,"pageNo":1,"totalCount":0}}}
                """;

        TourApiResponse<TourSyncItem> response =
                objectMapper.readValue(json, new TypeReference<TourApiResponse<TourSyncItem>>() {
                });

        assertThat(response.response().body().items()).isEmpty();
    }

    @Test
    void singleObjectItemDeserializesToOneElementList() throws Exception {
        String json = """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":{"contentid":"12345","title":"경복궁"}},
                "numOfRows":10,"pageNo":1,"totalCount":1}}}
                """;

        TourApiResponse<TourSyncItem> response =
                objectMapper.readValue(json, new TypeReference<TourApiResponse<TourSyncItem>>() {
                });

        assertThat(response.response().body().items()).hasSize(1);
        assertThat(response.response().body().items().get(0).contentid()).isEqualTo("12345");
        assertThat(response.response().body().items().get(0).title()).isEqualTo("경복궁");
    }

    @Test
    void arrayOfItemsDeserializesToList() throws Exception {
        String json = """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":[{"contentid":"1"},{"contentid":"2"}]},
                "numOfRows":10,"pageNo":1,"totalCount":2}}}
                """;

        TourApiResponse<TourSyncItem> response =
                objectMapper.readValue(json, new TypeReference<TourApiResponse<TourSyncItem>>() {
                });

        assertThat(response.response().body().items())
                .extracting(TourSyncItem::contentid)
                .containsExactly("1", "2");
    }
}
