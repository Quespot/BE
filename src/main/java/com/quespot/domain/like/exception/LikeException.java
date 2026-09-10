package com.quespot.domain.like.exception;

import com.quespot.domain.like.exception.code.LikeErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class LikeException extends GeneralException {
    public LikeException(LikeErrorCode errorCode) {
        super(errorCode);
    }
}
