package com.quespot.domain.tour.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.tour.client.dto.TourApiResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiClientTest {

    private MockRestServiceServer mockServer;
    private TourApiClient tourApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        tourApiClient = new TourApiClient(builder, objectMapper, "test-service-key");
    }

    @Test
    void parsesSuccessfulJsonResponse() {
        String json = """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":[{"contentid":"1"}]},"numOfRows":100,"pageNo":1,"totalCount":1}}}
                """;
        mockServer.expect(method(HttpMethod.GET)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        TourApiResponse<JsonNode> response = tourApiClient.fetchSyncList("11", 12, 1, 100, null);

        assertThat(response.response().body().items()).hasSize(1);
        assertThat(response.response().body().items().get(0).get("contentid").asText()).isEqualTo("1");
    }

    @Test
    void xmlGatewayErrorThrowsTourApiExceptionWithQuotaExceeded() {
        String xml = "<OpenAPI_ServiceResponse><cmmMsgHeader><returnReasonCode>22</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>";
        mockServer.expect(method(HttpMethod.GET)).andRespond(withSuccess(xml, MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> tourApiClient.fetchSyncList("11", 12, 1, 100, null))
                .isInstanceOf(TourApiException.class)
                .satisfies(e -> assertThat(((TourApiException) e).isQuotaExceeded()).isTrue());
    }

    @Test
    void nonSuccessResultCodeThrowsTourApiException() {
        String json = """
                {"response":{"header":{"resultCode":"0001","resultMsg":"APPLICATION ERROR"},
                "body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}
                """;
        mockServer.expect(method(HttpMethod.GET)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tourApiClient.fetchSyncList("11", 12, 1, 100, null))
                .isInstanceOf(TourApiException.class);
    }

    @Test
    void modifiedTimeQueryParamOmittedWhenNull() {
        mockServer.expect(requestTo(Matchers.not(Matchers.containsString("modifiedtime"))))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                        "body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}
                        """, MediaType.APPLICATION_JSON));

        tourApiClient.fetchSyncList("11", 12, 1, 100, null);

        mockServer.verify();
    }

    @Test
    void modifiedTimeQueryParamIncludedWhenPresent() {
        mockServer.expect(requestTo(Matchers.containsString("modifiedtime=20260101000000")))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                        "body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}
                        """, MediaType.APPLICATION_JSON));

        tourApiClient.fetchSyncList("11", 12, 1, 100, LocalDateTime.of(2026, 1, 1, 0, 0));

        mockServer.verify();
    }
}
