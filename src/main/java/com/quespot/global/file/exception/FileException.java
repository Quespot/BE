package com.quespot.global.file.exception;

import com.quespot.global.apiPayload.exception.GeneralException;
import com.quespot.global.file.exception.code.FileErrorCode;

public class FileException extends GeneralException {

    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }
}
