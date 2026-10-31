package com.quespot.domain.mission.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchivePhotoTest {

    @Test
    void uploadSetsAllFieldsAndCreatedAt() {
        ArchivePhoto photo = ArchivePhoto.upload(1L, "archives/1/abc.jpg", "여행 기록");

        assertThat(photo.getUserId()).isEqualTo(1L);
        assertThat(photo.getImageKey()).isEqualTo("archives/1/abc.jpg");
        assertThat(photo.getCaption()).isEqualTo("여행 기록");
        assertThat(photo.getCreatedAt()).isNotNull();
    }

    @Test
    void uploadAllowsNullCaption() {
        ArchivePhoto photo = ArchivePhoto.upload(1L, "archives/1/abc.jpg", null);

        assertThat(photo.getCaption()).isNull();
    }
}
