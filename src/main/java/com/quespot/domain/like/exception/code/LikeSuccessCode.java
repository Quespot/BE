package com.quespot.domain.like.exception.code;

import com.quespot.global.apiPayload.code.BaseCode;
import com.quespot.global.apiPayload.code.SuccessReasonDTO;
import org.springframework.http.HttpStatus;

public enum LikeSuccessCode implements BaseCode {
    LIKED(HttpStatus.OK,
            "LIKE_200_001",
            "좋아요를 등록했습니다."),
    UNLIKED(HttpStatus.OK,
            "LIKE_200_002",
            "좋아요를 해제했습니다."),
    LIKED_MISSIONS_FOUND(HttpStatus.OK,
            "LIKE_200_003",
            "좋아요한 미션 목록을 조회했습니다."),
    LIKED_COURSES_FOUND(HttpStatus.OK,
            "LIKE_200_004",
            "좋아요한 코스 목록을 조회했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    LikeSuccessCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public SuccessReasonDTO getReason() {
        return new SuccessReasonDTO(null, true, code, message);
    }

    @Override
    public SuccessReasonDTO getReasonHttpStatus() {
        return new SuccessReasonDTO(httpStatus, true, code, message);
    }
}
