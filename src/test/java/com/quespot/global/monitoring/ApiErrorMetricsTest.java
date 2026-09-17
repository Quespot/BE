package com.quespot.global.monitoring;

import com.quespot.global.apiPayload.code.GeneralErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorMetricsTest {

    @Test
    void incrementsCounterByErrorCodeAndStatus() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApiErrorMetrics apiErrorMetrics = new ApiErrorMetrics(meterRegistry);

        apiErrorMetrics.increment(GeneralErrorCode.COMMON_503_001);

        assertThat(meterRegistry.get("quespot.api.errors")
                .tags("code", "COMMON_503_001", "status", "503")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
