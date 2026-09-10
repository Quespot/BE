package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.repository.ArchivePhotoRepository;
import com.quespot.global.file.exception.FileException;
import com.quespot.global.file.exception.code.FileErrorCode;
import com.quespot.global.file.service.FileService;
import com.quespot.global.file.storage.FileStorage;
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
    private FileStorage fileStorage;
    private FileService fileService;
    private ArchivePhotoService archivePhotoService;

    @BeforeEach
    void setUp() {
        archivePhotoRepository = mock(ArchivePhotoRepository.class);
        fileStorage = mock(FileStorage.class);
        fileService = new FileService(fileStorage);
        archivePhotoService = new ArchivePhotoService(archivePhotoRepository, fileService);
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
                .isInstanceOf(FileException.class)
                .extracting(e -> ((FileException) e).getErrorCode())
                .isEqualTo(FileErrorCode.OBJECT_KEY_WRONG_PURPOSE);
    }

    @Test
    void throwsWhenObjectKeyBelongsToDifferentUser() {
        assertThatThrownBy(() -> archivePhotoService.registerPhoto(1L, "archives/2/abc.jpg", null))
                .isInstanceOf(FileException.class)
                .extracting(e -> ((FileException) e).getErrorCode())
                .isEqualTo(FileErrorCode.OBJECT_KEY_OWNER_MISMATCH);
    }

    @Test
    void resolveViewUrlReturnsNullWhenPhotoIsNull() {
        assertThat(archivePhotoService.resolveViewUrl(null)).isNull();
    }

    @Test
    void resolveViewUrlDelegatesToFileStorageWithStoredKey() {
        ArchivePhoto photo = ArchivePhoto.upload(1L, VALID_OBJECT_KEY, null);
        when(fileStorage.createPresignedDownloadUrl(VALID_OBJECT_KEY)).thenReturn("https://presigned-url");

        String viewUrl = archivePhotoService.resolveViewUrl(photo);

        assertThat(viewUrl).isEqualTo("https://presigned-url");
    }
}
