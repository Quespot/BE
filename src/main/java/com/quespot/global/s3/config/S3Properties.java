package com.quespot.global.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String bucket,
        String region,
        long presignedUrlExpirationSeconds,
        long maxFileSizeBytes
) {
}
