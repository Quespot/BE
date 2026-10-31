package com.quespot.global.apiPayload.handler;

import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import com.quespot.global.apiPayload.code.GeneralErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;
import com.quespot.global.monitoring.ApiErrorMetrics;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GeneralExceptionAdvice {

    private final ApiErrorMetrics apiErrorMetrics;

    @ExceptionHandler(GeneralException.class)
    protected ResponseEntity<ApiResponse<Void>> handleGeneralException(
            GeneralException exception,
            HttpServletRequest request
    ) {
        return handleExceptionInternal(exception.getErrorCode(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return handleExceptionInternal(GeneralErrorCode.COMMON_400_002, errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return handleExceptionInternal(
                GeneralErrorCode.COMMON_400_002,
                Map.of("message", exception.getMessage()),
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return handleExceptionInternal(
                GeneralErrorCode.COMMON_400_002,
                Map.of(exception.getName(), "요청 파라미터 형식이 올바르지 않습니다."),
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpServletRequest request
    ) {
        return handleExceptionInternal(GeneralErrorCode.COMMON_405_001, request);
    }

    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleDataAccessException(HttpServletRequest request) {
        return handleExceptionInternal(GeneralErrorCode.COMMON_503_001, request);
    }

    // 처리되지 않은 예외는 응답이 COMMON_500_001로만 나가서 원인을 알 수 없다(#67에서
    // LazyInitializationException이 서버 로그에도 안 남아 코드 추적으로만 찾았다).
    // 스택트레이스를 반드시 남긴다.
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("처리되지 않은 예외", exception);
        return handleExceptionInternal(GeneralErrorCode.COMMON_500_001, request);
    }

    private ResponseEntity<ApiResponse<Void>> handleExceptionInternal(
            BaseErrorCode errorCode,
            HttpServletRequest request
    ) {
        return handleExceptionInternal(errorCode, Map.of(), request);
    }

    private ResponseEntity<ApiResponse<Void>> handleExceptionInternal(
            BaseErrorCode errorCode,
            Object errorDetail,
            HttpServletRequest request
    ) {
        ErrorReasonDTO reason = errorCode.getReasonHttpStatus();
        apiErrorMetrics.increment(errorCode, request);

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponse.onFailure(errorCode, errorDetail));
    }
}
