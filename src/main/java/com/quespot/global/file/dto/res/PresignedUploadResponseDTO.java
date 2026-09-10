package com.quespot.global.file.dto.res;

import com.quespot.global.file.model.PresignedUploadResult;

import java.util.Map;

public record PresignedUploadResponseDTO(
        String objectKey,
        String uploadUrl,
        Map<String, String> requiredHeaders,
        long expiresInSeconds
) {

    public static PresignedUploadResponseDTO from(PresignedUploadResult result) {
        return new PresignedUploadResponseDTO(
                result.objectKey(),
                result.uploadUrl(),
                result.requiredHeaders(),
                result.expiresInSeconds()
        );
    }
}
