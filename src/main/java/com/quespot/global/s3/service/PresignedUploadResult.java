package com.quespot.global.s3.service;

import java.util.Map;

public record PresignedUploadResult(
        String objectKey,
        String uploadUrl,
        Map<String, String> requiredHeaders,
        long expiresInSeconds
) {
}
