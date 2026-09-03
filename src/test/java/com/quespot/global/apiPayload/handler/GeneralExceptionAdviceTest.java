package com.quespot.global.apiPayload.handler;

import com.quespot.global.apiPayload.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralExceptionAdviceTest {

    private final GeneralExceptionAdvice advice = new GeneralExceptionAdvice();

    @Test
    void mapsMethodArgumentTypeMismatchExceptionTo400() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("size");

        ResponseEntity<ApiResponse<Void>> response = advice.handleMethodArgumentTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getIsSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON_400_002");
    }
}
