package com.quespot.global.file.model;

import java.util.Map;

public record PresignedUploadResult(
        String objectKey,
        String uploadUrl,
        Map<String, String> requiredHeaders,
        long expiresInSeconds
) {
}
