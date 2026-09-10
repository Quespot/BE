package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.repository.ArchivePhotoRepository;
import com.quespot.global.s3.exception.S3Exception;
import com.quespot.global.s3.exception.code.S3ErrorCode;
import com.quespot.global.s3.service.S3ObjectKeyValidator;
import com.quespot.global.s3.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchivePhotoServiceTest {

    private static final String VALID_OBJECT_KEY = "archives/1/abc.jpg";

    private ArchivePhotoRepository archivePhotoRepository;
    private S3Service s3Service;
    private ArchivePhotoService archivePhotoService;

    @BeforeEach
    void setUp() {
        archivePhotoRepository = mock(ArchivePhotoRepository.class);
        s3Service = mock(S3Service.class);
        // 실제 인스턴스 — key 패턴 검증은 순수 로직이라 목킹 없이 그대로 검증 가능.
        S3ObjectKeyValidator s3ObjectKeyValidator = new S3ObjectKeyValidator();
        archivePhotoService = new ArchivePhotoService(archivePhotoRepository, s3ObjectKeyValidator, s3Service);
        when(archivePhotoRepository.save(any(ArchivePhoto.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registersPhotoWithGivenObjectKeyAndCaption() {
        ArchivePhoto photo = archivePhotoService.registerPhoto(1L, VALID_OBJECT_KEY, "혼자 찍음");

        assertThat(photo.getUserId()).isEqualTo(1L);
        assertThat(photo.getImageKey()).isEqualTo(VALID_OBJECT_KEY);
        assertThat(photo.getCaption()).isEqualTo("혼자 찍음");
    }

    @Test
    void registerPhotoReturnsExistingRowWhenSameObjectKeyIsResubmitted() {
        ArchivePhoto existing = ArchivePhoto.upload(1L, VALID_OBJECT_KEY, "먼저 등록됨");
        when(archivePhotoRepository.findByUserIdAndImageKey(1L, VALID_OBJECT_KEY)).thenReturn(java.util.Optional.of(existing));

        ArchivePhoto photo = archivePhotoService.registerPhoto(1L, VALID_OBJECT_KEY, "재시도로 다시 옴");

        assertThat(photo).isSameAs(existing);
        org.mockito.Mockito.verify(archivePhotoRepository, org.mockito.Mockito.never()).save(any(ArchivePhoto.class));
    }

    @Test
    void registersPhotoWithNullCaption() {
        ArchivePhoto photo = archivePhotoService.registerPhoto(1L, VALID_OBJECT_KEY, null);

        assertThat(photo.getCaption()).isNull();
    }

    @Test
    void throwsWhenObjectKeyIsWrongPurpose() {
        assertThatThrownBy(() -> archivePhotoService.registerPhoto(1L, "missions/1/abc.jpg", null))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.OBJECT_KEY_WRONG_PURPOSE);
    }

    @Test
    void throwsWhenObjectKeyBelongsToDifferentUser() {
        assertThatThrownBy(() -> archivePhotoService.registerPhoto(1L, "archives/2/abc.jpg", null))
                .isInstanceOf(S3Exception.class)
                .extracting(e -> ((S3Exception) e).getErrorCode())
                .isEqualTo(S3ErrorCode.OBJECT_KEY_OWNER_MISMATCH);
    }

    @Test
    void resolveViewUrlReturnsNullWhenPhotoIsNull() {
        assertThat(archivePhotoService.resolveViewUrl(null)).isNull();
    }

    @Test
    void resolveViewUrlDelegatesToS3ServiceWithStoredKey() {
        ArchivePhoto photo = ArchivePhoto.upload(1L, VALID_OBJECT_KEY, null);
        when(s3Service.createPresignedDownloadUrl(VALID_OBJECT_KEY)).thenReturn("https://presigned-url");

        String viewUrl = archivePhotoService.resolveViewUrl(photo);

        assertThat(viewUrl).isEqualTo("https://presigned-url");
    }
}
