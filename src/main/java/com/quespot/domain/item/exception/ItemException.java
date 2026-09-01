package com.quespot.domain.item.exception;

import com.quespot.domain.item.exception.code.ItemErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class ItemException extends GeneralException {

    public ItemException(ItemErrorCode errorCode) {
        super(errorCode);
    }
}
