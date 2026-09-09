package com.quespot.global.s3.service;

import com.quespot.global.s3.config.S3Properties;
import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ServiceTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = mock(S3Presigner.class);
        S3Properties properties = new S3Properties(
                "quespot-test",
                "ap-northeast-2",
                300,
                10 * 1024 * 1024
        );
        s3Service = new S3Service(s3Client, s3Presigner, properties);
    }

    @Test
    void createsPresignedUploadUrlForAllowedImage() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/upload"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        PresignedUploadResult result = s3Service.createPresignedUploadUrl(
                1L,
                UploadPurpose.MISSION,
                "archive.jpeg",
                "image/jpeg",
                1024
        );

        assertThat(result.objectKey()).startsWith("missions/1/").endsWith(".jpg");
        assertThat(result.uploadUrl()).isEqualTo("https://example.com/upload");
        assertThat(result.requiredHeaders()).containsEntry("Content-Type", "image/jpeg");
        assertThat(result.expiresInSeconds()).isEqualTo(300);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofSeconds(300));
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("quespot-test");
        assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/jpeg");
        assertThat(captor.getValue().putObjectRequest().contentLength()).isEqualTo(1024L);
    }

    @Test
    void rejectsUnsupportedContentType() {
        assertThatThrownBy(() -> s3Service.createPresignedUploadUrl(
                1L,
                UploadPurpose.PROFILE,
                "profile.gif",
                "image/gif",
                1024
        )).isInstanceOf(S3Exception.class)
                .extracting(exception -> ((S3Exception) exception).getErrorCode())
                .isEqualTo(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    @Test
    void rejectsExtensionThatDoesNotMatchContentType() {
        assertThatThrownBy(() -> s3Service.createPresignedUploadUrl(
                1L,
                UploadPurpose.PROFILE,
                "profile.png",
                "image/jpeg",
                1024
        )).isInstanceOf(S3Exception.class)
                .extracting(exception -> ((S3Exception) exception).getErrorCode())
                .isEqualTo(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    @Test
    void rejectsFileLargerThanConfiguredLimit() {
        assertThatThrownBy(() -> s3Service.createPresignedUploadUrl(
                1L,
                UploadPurpose.PROFILE,
                "profile.webp",
                "image/webp",
                10 * 1024 * 1024L + 1
        )).isInstanceOf(S3Exception.class)
                .extracting(exception -> ((S3Exception) exception).getErrorCode())
                .isEqualTo(S3ErrorCode.INVALID_FILE_SIZE);
    }

    @Test
    void deletesObjectFromConfiguredBucket() {
        s3Service.deleteObject("profiles/1/profile.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("quespot-test");
        assertThat(captor.getValue().key()).isEqualTo("profiles/1/profile.jpg");
    }

    @Test
    void createsPresignedDownloadUrlForManagedObject() throws MalformedURLException {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/download"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        String downloadUrl = s3Service.createPresignedDownloadUrl("archives/1/archive.jpg");

        assertThat(downloadUrl).isEqualTo("https://example.com/download");
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("quespot-test");
        assertThat(captor.getValue().getObjectRequest().key())
                .isEqualTo("archives/1/archive.jpg");
    }

    @Test
    void createsArchiveObjectKey() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/upload"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        PresignedUploadResult result = s3Service.createPresignedUploadUrl(
                1L,
                UploadPurpose.ARCHIVE,
                "memory.webp",
                "image/webp",
                1024
        );

        assertThat(result.objectKey()).startsWith("archives/1/").endsWith(".webp");
    }
}
