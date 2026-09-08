package com.quespot.domain.mission.exception;

import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class MissionException extends GeneralException {

    public MissionException(MissionErrorCode errorCode) {
        super(errorCode);
    }
}
