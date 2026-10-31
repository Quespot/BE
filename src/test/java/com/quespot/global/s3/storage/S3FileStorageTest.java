package com.quespot.global.s3.storage;

import com.quespot.global.file.model.PresignedUploadResult;
import com.quespot.global.s3.config.S3Properties;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3FileStorageTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3FileStorage fileStorage;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = mock(S3Presigner.class);
        S3Properties properties = new S3Properties(
                "quespot-test", "ap-northeast-2", 300, 10 * 1024 * 1024
        );
        fileStorage = new S3FileStorage(s3Client, s3Presigner, properties);
    }

    @Test
    void createsPresignedUploadUrl() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/upload"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        PresignedUploadResult result = fileStorage.createPresignedUploadUrl(
                "missions/1/archive.jpg", "image/jpeg", 1024
        );

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
    void createsPresignedDownloadUrl() throws MalformedURLException {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/download"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        String downloadUrl = fileStorage.createPresignedDownloadUrl("archives/1/archive.jpg");

        assertThat(downloadUrl).isEqualTo("https://example.com/download");
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("quespot-test");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("archives/1/archive.jpg");
    }

    @Test
    void deletesObjectFromConfiguredBucket() {
        fileStorage.deleteObject("profiles/1/profile.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("quespot-test");
        assertThat(captor.getValue().key()).isEqualTo("profiles/1/profile.jpg");
    }
}
