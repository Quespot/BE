package com.quespot.global.s3.service;

import com.quespot.global.s3.config.S3Properties;
import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final long MAX_PRESIGNED_URL_EXPIRATION_SECONDS = 7 * 24 * 60 * 60;

    private static final Map<String, Set<String>> ALLOWED_IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp")
    );

    private static final Set<String> ALLOWED_ROOT_DIRECTORIES = Set.of(
            UploadPurpose.PROFILE.getDirectory(),
            UploadPurpose.MISSION.getDirectory(),
            UploadPurpose.ARCHIVE.getDirectory()
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public PresignedUploadResult createPresignedUploadUrl(
            Long userId,
            UploadPurpose purpose,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
        validateConfiguration();
        String normalizedContentType = normalizeContentType(contentType);
        String extension = validateAndExtractExtension(originalFilename, normalizedContentType);
        validateFileSize(fileSize);

        String objectKey = createObjectKey(userId, purpose, extension);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(normalizedContentType)
                .contentLength(fileSize)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration())
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUploadResult(
                    objectKey,
                    presignedRequest.url().toString(),
                    Map.of("Content-Type", normalizedContentType),
                    properties.presignedUrlExpirationSeconds()
            );
        } catch (SdkException e) {
            throw new S3Exception(S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    public String createPresignedDownloadUrl(String objectKey) {
        validateConfiguration();
        validateObjectKey(objectKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (SdkException e) {
            throw new S3Exception(S3ErrorCode.PRESIGNED_DOWNLOAD_URL_GENERATION_FAILED);
        }
    }

    public void deleteObject(String objectKey) {
        validateConfiguration();
        validateObjectKey(objectKey);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new S3Exception(S3ErrorCode.OBJECT_DELETE_FAILED);
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new S3Exception(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.containsKey(normalized)) {
            throw new S3Exception(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
        return normalized;
    }

    private String validateAndExtractExtension(String originalFilename, String contentType) {
        if (originalFilename == null) {
            throw new S3Exception(S3ErrorCode.INVALID_FILE_NAME);
        }

        String filename = originalFilename.trim();
        int extensionSeparator = filename.lastIndexOf('.');
        if (filename.isEmpty()
                || filename.length() > 255
                || filename.contains("/")
                || filename.contains("\\")
                || extensionSeparator <= 0
                || extensionSeparator == filename.length() - 1) {
            throw new S3Exception(S3ErrorCode.INVALID_FILE_NAME);
        }

        String extension = filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.get(contentType).contains(extension)) {
            throw new S3Exception(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    private void validateFileSize(long fileSize) {
        if (fileSize <= 0 || fileSize > properties.maxFileSizeBytes()) {
            throw new S3Exception(S3ErrorCode.INVALID_FILE_SIZE);
        }
    }

    private String createObjectKey(Long userId, UploadPurpose purpose, String extension) {
        if (userId == null || userId <= 0 || purpose == null) {
            throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
        }
        return "%s/%d/%s.%s".formatted(
                purpose.getDirectory(),
                userId,
                UUID.randomUUID(),
                extension
        );
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null
                || objectKey.isBlank()
                || objectKey.startsWith("/")
                || objectKey.contains("..")
                || objectKey.contains("\\")) {
            throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
        }

        int separator = objectKey.indexOf('/');
        if (separator <= 0 || !ALLOWED_ROOT_DIRECTORIES.contains(objectKey.substring(0, separator))) {
            throw new S3Exception(S3ErrorCode.INVALID_OBJECT_KEY);
        }
    }

    private void validateConfiguration() {
        if (properties.bucket() == null
                || properties.bucket().isBlank()
                || properties.presignedUrlExpirationSeconds() <= 0
                || properties.presignedUrlExpirationSeconds() > MAX_PRESIGNED_URL_EXPIRATION_SECONDS
                || properties.maxFileSizeBytes() <= 0) {
            throw new S3Exception(S3ErrorCode.S3_NOT_CONFIGURED);
        }
    }

    private Duration expiration() {
        return Duration.ofSeconds(properties.presignedUrlExpirationSeconds());
    }
}
