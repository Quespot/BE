package com.quespot.domain.reward.exception;

import com.quespot.domain.reward.exception.code.RewardErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;

public class RewardException extends GeneralException {
    public RewardException(RewardErrorCode errorCode) {
        super(errorCode);
    }
}
