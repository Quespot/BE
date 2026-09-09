package com.quespot.global.s3.exception;

import com.quespot.global.apiPayload.exception.GeneralException;
import com.quespot.global.s3.exception.code.S3ErrorCode;

public class S3Exception extends GeneralException {

    public S3Exception(S3ErrorCode errorCode) {
        super(errorCode);
    }
}
