package com.quespot.domain.tour.exception;

import com.quespot.domain.tour.exception.code.TourSyncErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class TourSyncException extends GeneralException {

    public TourSyncException(TourSyncErrorCode errorCode) {
        super(errorCode);
    }
}
