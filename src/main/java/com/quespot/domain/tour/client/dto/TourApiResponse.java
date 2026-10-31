package com.quespot.domain.tour.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

public record TourApiResponse<T>(
        @JsonProperty("response") Response<T> response
) {

    public record Response<T>(
            @JsonProperty("header") Header header,
            @JsonProperty("body") Body<T> body
    ) {
    }

    public record Header(
            @JsonProperty("resultCode") String resultCode,
            @JsonProperty("resultMsg") String resultMsg
    ) {
    }

    public record Body<T>(
            @JsonProperty("items") @JsonDeserialize(using = TourApiItemsDeserializer.class) List<T> items,
            @JsonProperty("numOfRows") int numOfRows,
            @JsonProperty("pageNo") int pageNo,
            @JsonProperty("totalCount") long totalCount
    ) {
    }
}
