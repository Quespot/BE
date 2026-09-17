package com.quespot.global.monitoring;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiErrorMetrics {

    private static final String METRIC_NAME = "quespot.api.errors";

    private final MeterRegistry meterRegistry;

    public void increment(BaseErrorCode errorCode) {
        ErrorReasonDTO reason = errorCode.getReasonHttpStatus();

        meterRegistry.counter(
                METRIC_NAME,
                "code", reason.getCode(),
                "status", String.valueOf(reason.getHttpStatus().value())
        ).increment();
    }
}
