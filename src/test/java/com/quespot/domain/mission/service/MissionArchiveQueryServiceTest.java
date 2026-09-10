package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.enums.ArchivePhotoSource;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.ArchivePhotoRepository;
import com.quespot.domain.mission.repository.projection.ArchiveFeedRowProjection;
import com.quespot.global.file.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionArchiveQueryServiceTest {

    private ArchivePhotoRepository archivePhotoRepository;
    private ArchiveCursorCodec archiveCursorCodec;
    private FileService fileService;
    private MissionArchiveQueryService service;

    @BeforeEach
    void setUp() {
        archivePhotoRepository = mock(ArchivePhotoRepository.class);
        archiveCursorCodec = mock(ArchiveCursorCodec.class);
        fileService = mock(FileService.class);
        service = new MissionArchiveQueryService(archivePhotoRepository, archiveCursorCodec, fileService);
        when(fileService.createPresignedDownloadUrl(org.mockito.ArgumentMatchers.anyString())).thenReturn("https://presigned-url");
    }

    private ArchiveFeedRowProjection rowAt(Long id, ArchivePhotoSource source, LocalDateTime createdAt) {
        ArchiveFeedRowProjection row = mock(ArchiveFeedRowProjection.class);
        when(row.getId()).thenReturn(id);
        when(row.getSource()).thenReturn(source.name());
        when(row.getImageKey()).thenReturn("missions/1/" + id + ".jpg");
        when(row.getCreatedAt()).thenReturn(createdAt);
        if (source == ArchivePhotoSource.MISSION) {
            when(row.getMissionId()).thenReturn(200L + id);
            when(row.getMissionTitle()).thenReturn("미션 " + id);
            when(row.getMissionCategory()).thenReturn("CULTURE");
            when(row.getCompletedAt()).thenReturn(createdAt.minusMinutes(5));
        }
        return row;
    }

    @Test
    void getArchivesFirstPageQueriesWithNullCursor() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);
        ArchiveFeedRowProjection r1 = rowAt(1L, ArchivePhotoSource.MISSION, now);
        when(archivePhotoRepository.findFeedPage(eq(1L), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(r1));

        MissionArchiveListResponseDTO result = service.getArchives(1L, null, 20);

        assertThat(result.archives()).hasSize(1);
        assertThat(result.archives().get(0).photoId()).isEqualTo(1L);
        assertThat(result.archives().get(0).source()).isEqualTo(ArchivePhotoSource.MISSION);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void getArchivesSetsHasNextAndTrimsExtraRowWhenMoreThanSizeReturned() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);
        ArchiveFeedRowProjection r1 = rowAt(1L, ArchivePhotoSource.MISSION, now);
        ArchiveFeedRowProjection r2 = rowAt(2L, ArchivePhotoSource.ARCHIVE, now.minusMinutes(1));
        ArchiveFeedRowProjection r3 = rowAt(3L, ArchivePhotoSource.ARCHIVE, now.minusMinutes(2));
        when(archivePhotoRepository.findFeedPage(eq(1L), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(List.of(r1, r2, r3));
        when(archiveCursorCodec.encode(any(ArchiveCursor.class))).thenReturn("encoded-cursor");

        MissionArchiveListResponseDTO result = service.getArchives(1L, null, 2);

        assertThat(result.archives()).hasSize(2);
        assertThat(result.archives()).extracting("photoId").containsExactly(1L, 2L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("encoded-cursor");
    }

    @Test
    void getArchivesDecodesGivenCursorAndPassesItToRepository() {
        LocalDateTime cursorTime = LocalDateTime.of(2026, 9, 10, 9, 0);
        ArchiveCursor cursor = new ArchiveCursor(cursorTime, ArchivePhotoSource.ARCHIVE, 5L);
        when(archiveCursorCodec.decode("some-cursor")).thenReturn(cursor);
        when(archivePhotoRepository.findFeedPage(eq(1L), eq(cursorTime), eq("ARCHIVE"), eq(5L), anyInt()))
                .thenReturn(List.of());

        service.getArchives(1L, "some-cursor", 20);

        verify(archivePhotoRepository).findFeedPage(eq(1L), eq(cursorTime), eq("ARCHIVE"), eq(5L), anyInt());
    }

    @Test
    void getArchivesThrowsInvalidCursorWhenCodecRejectsIt() {
        when(archiveCursorCodec.decode("garbage"))
                .thenThrow(new MissionException(MissionErrorCode.INVALID_CURSOR));

        assertThatThrownBy(() -> service.getArchives(1L, "garbage", 20))
                .isInstanceOf(MissionException.class)
                .extracting(e -> ((MissionException) e).getErrorCode())
                .isEqualTo(MissionErrorCode.INVALID_CURSOR);
    }
}
