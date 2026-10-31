package com.quespot.domain.user.exception;

import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class AuthException extends GeneralException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
