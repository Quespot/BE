package com.quespot.domain.like.exception.code;

import com.quespot.global.apiPayload.code.BaseErrorCode;
import com.quespot.global.apiPayload.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;

// spot 도메인에 exception 패키지가 아직 없어(허건우 조회 API 미착수) 스팟 오류를
// 여기 둔다. 허건우가 SpotErrorCode를 만들면 그쪽으로 옮긴다.
public enum LikeErrorCode implements BaseErrorCode {
    SPOT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "LIKE_404_001",
            "스팟을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    LikeErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public ErrorReasonDTO getReason() {
        return new ErrorReasonDTO(null, false, code, message);
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return new ErrorReasonDTO(httpStatus, false, code, message);
    }
}
