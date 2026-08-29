package com.quespot.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, BaseErrorCode errorCode) throws IOException {
        ErrorReasonDTO reason = errorCode.getReasonHttpStatus();

        response.setStatus(reason.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.onFailure(errorCode));
    }
}
