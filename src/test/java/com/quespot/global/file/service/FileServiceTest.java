package com.quespot.global.file.service;

import com.quespot.global.file.enums.UploadPurpose;
import com.quespot.global.file.exception.FileException;
import com.quespot.global.file.exception.code.FileErrorCode;
import com.quespot.global.file.model.PresignedUploadResult;
import com.quespot.global.file.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private FileStorage fileStorage;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileStorage = mock(FileStorage.class);
        when(fileStorage.maxFileSizeBytes()).thenReturn(10 * 1024 * 1024L);
        fileService = new FileService(fileStorage);
    }

    @Test
    void createsPresignedUploadUrlForAllowedImage() {
        when(fileStorage.createPresignedUploadUrl(anyString(), eq("image/jpeg"), eq(1024L)))
                .thenAnswer(invocation -> new PresignedUploadResult(
                        invocation.getArgument(0),
                        "https://example.com/upload",
                        Map.of("Content-Type", "image/jpeg"),
                        300
                ));

        PresignedUploadResult result = fileService.createPresignedUploadUrl(
                1L, UploadPurpose.MISSION, "archive.jpeg", "image/jpeg", 1024
        );

        assertThat(result.objectKey()).startsWith("missions/1/").endsWith(".jpg");
        assertThat(result.uploadUrl()).isEqualTo("https://example.com/upload");
        verify(fileStorage).createPresignedUploadUrl(result.objectKey(), "image/jpeg", 1024);
    }

    @Test
    void rejectsUnsupportedContentType() {
        assertFileError(() -> fileService.createPresignedUploadUrl(
                1L, UploadPurpose.PROFILE, "profile.gif", "image/gif", 1024
        ), FileErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    @Test
    void rejectsExtensionThatDoesNotMatchContentType() {
        assertFileError(() -> fileService.createPresignedUploadUrl(
                1L, UploadPurpose.PROFILE, "profile.png", "image/jpeg", 1024
        ), FileErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    @Test
    void rejectsFileLargerThanConfiguredLimit() {
        assertFileError(() -> fileService.createPresignedUploadUrl(
                1L, UploadPurpose.PROFILE, "profile.webp", "image/webp", 10 * 1024 * 1024L + 1
        ), FileErrorCode.INVALID_FILE_SIZE);
    }

    @Test
    void delegatesObjectDeletion() {
        fileService.deleteObject("profiles/1/profile.jpg");

        verify(fileStorage).deleteObject("profiles/1/profile.jpg");
    }

    @Test
    void delegatesPresignedDownloadUrlCreation() {
        when(fileStorage.createPresignedDownloadUrl("archives/1/archive.jpg"))
                .thenReturn("https://example.com/download");

        String downloadUrl = fileService.createPresignedDownloadUrl("archives/1/archive.jpg");

        assertThat(downloadUrl).isEqualTo("https://example.com/download");
    }

    @Test
    void createsArchiveObjectKey() {
        when(fileStorage.createPresignedUploadUrl(anyString(), eq("image/webp"), eq(1024L)))
                .thenAnswer(invocation -> new PresignedUploadResult(
                        invocation.getArgument(0),
                        "https://example.com/upload",
                        Map.of("Content-Type", "image/webp"),
                        300
                ));

        PresignedUploadResult result = fileService.createPresignedUploadUrl(
                1L, UploadPurpose.ARCHIVE, "memory.webp", "image/webp", 1024
        );

        assertThat(result.objectKey()).startsWith("archives/1/").endsWith(".webp");
    }

    @Test
    void acceptsObjectKeyOwnedByUserForPurpose() {
        fileService.validateOwnedObjectKey(1L, UploadPurpose.PROFILE, "profiles/1/profile.jpg");
    }

    @Test
    void rejectsObjectKeyOwnedByAnotherUser() {
        assertFileError(() -> fileService.validateOwnedObjectKey(
                1L, UploadPurpose.PROFILE, "profiles/2/profile.jpg"
        ), FileErrorCode.INVALID_OBJECT_KEY);
    }

    @Test
    void rejectsObjectKeyForAnotherPurpose() {
        assertFileError(() -> fileService.validateOwnedObjectKey(
                1L, UploadPurpose.PROFILE, "archives/1/archive.jpg"
        ), FileErrorCode.INVALID_OBJECT_KEY);
    }

    private void assertFileError(Runnable operation, FileErrorCode expectedErrorCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(FileException.class)
                .extracting(exception -> ((FileException) exception).getErrorCode())
                .isEqualTo(expectedErrorCode);
    }
}
