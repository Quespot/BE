package com.quespot.global.monitoring;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class ApiErrorMetrics {

    private static final String METRIC_NAME = "quespot.api.errors";
    private static final String UNKNOWN_METHOD = "UNKNOWN";
    private static final String UNMATCHED_URI = "UNMATCHED";

    private final MeterRegistry meterRegistry;

    public void increment(BaseErrorCode errorCode) {
        increment(errorCode, UNKNOWN_METHOD, UNMATCHED_URI);
    }

    public void increment(BaseErrorCode errorCode, HttpServletRequest request) {
        Object matchedPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String uri = matchedPattern == null ? UNMATCHED_URI : matchedPattern.toString();
        RequestMethod requestMethod = RequestMethod.resolve(request.getMethod());
        String method = requestMethod == null ? UNKNOWN_METHOD : requestMethod.name();

        increment(errorCode, method, uri);
    }

    private void increment(BaseErrorCode errorCode, String method, String uri) {
        ErrorReasonDTO reason = errorCode.getReasonHttpStatus();

        meterRegistry.counter(
                METRIC_NAME,
                "code", reason.getCode(),
                "status", String.valueOf(reason.getHttpStatus().value()),
                "method", method,
                "uri", uri
        ).increment();
    }
}
