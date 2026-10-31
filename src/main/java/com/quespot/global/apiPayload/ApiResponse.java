package com.quespot.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import com.quespot.global.apiPayload.code.GeneralSuccessCode;

import java.util.Collections;

@JsonPropertyOrder({"isSuccess", "code", "message", "result", "errorDetail"})
public final class ApiResponse<T> {

    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final T result;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Object errorDetail;

    private ApiResponse(
            Boolean isSuccess,
            String code,
            String message,
            T result,
            Object errorDetail
    ) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
        this.errorDetail = errorDetail;
    }

    public static ApiResponse<Void> onSuccess() {
        return ApiResponse.<Void>of(GeneralSuccessCode.COMMON_200, null);
    }

    public static <T> ApiResponse<T> onSuccess(T result) {
        return of(GeneralSuccessCode.COMMON_200, result);
    }

    public static <T> ApiResponse<T> of(BaseCode code, T result) {
        SuccessReasonDTO reason = code.getReason();

        return new ApiResponse<>(
                reason.getIsSuccess(),
                reason.getCode(),
                reason.getMessage(),
                result,
                null
        );
    }

    public static ApiResponse<Void> onFailure(BaseErrorCode errorCode) {
        return onFailure(errorCode, Collections.emptyMap());
    }

    public static ApiResponse<Void> onFailure(BaseErrorCode errorCode, Object errorDetail) {
        ErrorReasonDTO reason = errorCode.getReason();

        return new ApiResponse<>(
                reason.getIsSuccess(),
                reason.getCode(),
                reason.getMessage(),
                null,
                errorDetail
        );
    }

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getResult() {
        return result;
    }

    public Object getErrorDetail() {
        return errorDetail;
    }
}
