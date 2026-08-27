package com.quespot.global.apiPayload.handler;

import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import com.quespot.global.apiPayload.code.GeneralErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GeneralExceptionAdvice {

    @ExceptionHandler(GeneralException.class)
    protected ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException exception) {
        return handleExceptionInternal(exception.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return handleExceptionInternal(GeneralErrorCode.COMMON_400_002, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        return handleExceptionInternal(
                GeneralErrorCode.COMMON_400_002,
                Map.of("message", exception.getMessage())
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException() {
        return handleExceptionInternal(GeneralErrorCode.COMMON_405_001);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException() {
        return handleExceptionInternal(GeneralErrorCode.COMMON_500_001);
    }

    private ResponseEntity<ApiResponse<Void>> handleExceptionInternal(BaseErrorCode errorCode) {
        return handleExceptionInternal(errorCode, Map.of());
    }

    private ResponseEntity<ApiResponse<Void>> handleExceptionInternal(
            BaseErrorCode errorCode,
            Object errorDetail
    ) {
        ErrorReasonDTO reason = errorCode.getReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponse.onFailure(errorCode, errorDetail));
    }
}
