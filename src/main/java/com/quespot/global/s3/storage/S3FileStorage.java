package com.quespot.global.s3.storage;

import com.quespot.global.file.exception.FileException;
import com.quespot.global.file.exception.code.FileErrorCode;
import com.quespot.global.file.model.PresignedUploadResult;
import com.quespot.global.file.storage.FileStorage;
import com.quespot.global.s3.config.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
import java.util.Map;

@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

    private static final long MAX_PRESIGNED_URL_EXPIRATION_SECONDS = 7 * 24 * 60 * 60;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    @Override
    public PresignedUploadResult createPresignedUploadUrl(
            String objectKey,
            String contentType,
            long fileSize
    ) {
        validateConfiguration();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
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
                    Map.of("Content-Type", contentType),
                    properties.presignedUrlExpirationSeconds()
            );
        } catch (SdkException exception) {
            throw new FileException(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    @Override
    public String createPresignedDownloadUrl(String objectKey) {
        validateConfiguration();
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
        } catch (SdkException exception) {
            throw new FileException(FileErrorCode.PRESIGNED_DOWNLOAD_URL_GENERATION_FAILED);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        validateConfiguration();
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            throw new FileException(FileErrorCode.OBJECT_DELETE_FAILED);
        }
    }

    @Override
    public long maxFileSizeBytes() {
        validateConfiguration();
        return properties.maxFileSizeBytes();
    }

    private void validateConfiguration() {
        if (properties.bucket() == null
                || properties.bucket().isBlank()
                || properties.presignedUrlExpirationSeconds() <= 0
                || properties.presignedUrlExpirationSeconds() > MAX_PRESIGNED_URL_EXPIRATION_SECONDS
                || properties.maxFileSizeBytes() <= 0) {
            throw new FileException(FileErrorCode.FILE_STORAGE_NOT_CONFIGURED);
        }
    }

    private Duration expiration() {
        return Duration.ofSeconds(properties.presignedUrlExpirationSeconds());
    }
}
