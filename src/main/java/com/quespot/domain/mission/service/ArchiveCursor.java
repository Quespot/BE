package com.quespot.domain.mission.service;

import com.quespot.domain.mission.enums.ArchivePhotoSource;

import java.time.LocalDateTime;

record ArchiveCursor(LocalDateTime createdAt, ArchivePhotoSource source, Long photoId) {
}
