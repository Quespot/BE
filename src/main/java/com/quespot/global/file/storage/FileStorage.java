package com.quespot.global.file.storage;

import com.quespot.global.file.model.PresignedUploadResult;

public interface FileStorage {

    PresignedUploadResult createPresignedUploadUrl(
            String objectKey,
            String contentType,
            long fileSize
    );

    String createPresignedDownloadUrl(String objectKey);

    void deleteObject(String objectKey);

    long maxFileSizeBytes();
}
