package com.quespot.domain.user.exception;

import com.quespot.domain.user.exception.code.ProfileErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class ProfileException extends GeneralException {

    public ProfileException(ProfileErrorCode errorCode) {
        super(errorCode);
    }
}
