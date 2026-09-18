package com.quespot.global.monitoring;

import com.quespot.global.apiPayload.code.GeneralErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorMetricsTest {

    @Test
    void incrementsCounterWithFallbackTagsWithoutRequest() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApiErrorMetrics apiErrorMetrics = new ApiErrorMetrics(meterRegistry);

        apiErrorMetrics.increment(GeneralErrorCode.COMMON_503_001);

        assertCounter(meterRegistry, "UNKNOWN", "UNMATCHED");
    }

    @Test
    void incrementsCounterWithUnmatchedUriWithoutMatchedPattern() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApiErrorMetrics apiErrorMetrics = new ApiErrorMetrics(meterRegistry);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/unmatched");

        apiErrorMetrics.increment(GeneralErrorCode.COMMON_503_001, request);

        assertCounter(meterRegistry, "POST", "UNMATCHED");
    }

    @Test
    void incrementsCounterWithUnknownMethodForUnrecognizedHttpMethod() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApiErrorMetrics apiErrorMetrics = new ApiErrorMetrics(meterRegistry);
        MockHttpServletRequest request = new MockHttpServletRequest("CUSTOM", "/api/missions");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/missions");

        apiErrorMetrics.increment(GeneralErrorCode.COMMON_503_001, request);

        assertCounter(meterRegistry, "UNKNOWN", "/api/missions");
    }

    @Test
    void incrementsCounterByErrorCodeStatusMethodAndMatchedUri() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApiErrorMetrics apiErrorMetrics = new ApiErrorMetrics(meterRegistry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/missions/1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/missions/{missionId}");

        apiErrorMetrics.increment(GeneralErrorCode.COMMON_503_001, request);

        assertCounter(meterRegistry, "GET", "/api/missions/{missionId}");
    }

    private void assertCounter(SimpleMeterRegistry meterRegistry, String method, String uri) {
        assertThat(meterRegistry.get("quespot.api.errors")
                .tags(
                        "code", "COMMON_503_001",
                        "status", "503",
                        "method", method,
                        "uri", uri
                )
                .counter()
                .count()).isEqualTo(1.0);
    }
}
