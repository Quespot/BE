package com.quespot.domain.notification.exception;

import com.quespot.domain.notification.exception.code.NotificationErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class NotificationException extends GeneralException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
