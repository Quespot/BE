package com.quespot.global.monitoring;

import com.quespot.global.apiPayload.code.GeneralErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorMetricsTest {

    @Test
    void incrementsCounterByErrorCodeStatusMethodAndMatchedUri() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApiErrorMetrics apiErrorMetrics = new ApiErrorMetrics(meterRegistry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/missions/1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/missions/{missionId}");

        apiErrorMetrics.increment(GeneralErrorCode.COMMON_503_001, request);

        assertThat(meterRegistry.get("quespot.api.errors")
                .tags(
                        "code", "COMMON_503_001",
                        "status", "503",
                        "method", "GET",
                        "uri", "/api/missions/{missionId}"
                )
                .counter()
                .count()).isEqualTo(1.0);
    }
}
