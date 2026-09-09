package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionArchiveQueryServiceTest {

    private MissionPhotoRepository missionPhotoRepository;
    private ArchiveCursorCodec archiveCursorCodec;
    private MissionArchiveQueryService service;

    @BeforeEach
    void setUp() {
        missionPhotoRepository = mock(MissionPhotoRepository.class);
        archiveCursorCodec = mock(ArchiveCursorCodec.class);
        service = new MissionArchiveQueryService(missionPhotoRepository, archiveCursorCodec);
    }

    private MissionPhoto photoAt(Long id, LocalDateTime createdAt) {
        MissionPhoto photo = mock(MissionPhoto.class);
        MissionAttempt attempt = mock(MissionAttempt.class);
        Mission mission = mock(Mission.class);
        when(photo.getId()).thenReturn(id);
        when(photo.getImageUrl()).thenReturn("https://test-bucket.s3.ap-northeast-2.amazonaws.com/missions/1/" + id + ".jpg");
        when(photo.getCreatedAt()).thenReturn(createdAt);
        when(photo.getAttempt()).thenReturn(attempt);
        when(attempt.getMission()).thenReturn(mission);
        when(attempt.getCompletedAt()).thenReturn(createdAt.minusMinutes(5));
        when(mission.getId()).thenReturn(200L + id);
        when(mission.getTitle()).thenReturn("미션 " + id);
        return photo;
    }

    @Test
    void getArchivesFirstPageQueriesWithNullCursor() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);
        MissionPhoto p1 = photoAt(1L, now);
        when(missionPhotoRepository.findArchivePage(eq(1L), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(List.of(p1));

        MissionArchiveListResponseDTO result = service.getArchives(1L, null, 20);

        assertThat(result.archives()).hasSize(1);
        assertThat(result.archives().get(0).photoId()).isEqualTo(1L);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void getArchivesSetsHasNextAndTrimsExtraRowWhenMoreThanSizeReturned() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);
        MissionPhoto p1 = photoAt(1L, now);
        MissionPhoto p2 = photoAt(2L, now.minusMinutes(1));
        MissionPhoto p3 = photoAt(3L, now.minusMinutes(2));
        when(missionPhotoRepository.findArchivePage(eq(1L), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(List.of(p1, p2, p3));
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
        ArchiveCursor cursor = new ArchiveCursor(cursorTime, 5L);
        when(archiveCursorCodec.decode("some-cursor")).thenReturn(cursor);
        when(missionPhotoRepository.findArchivePage(eq(1L), eq(cursorTime), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of());

        service.getArchives(1L, "some-cursor", 20);

        verify(missionPhotoRepository).findArchivePage(eq(1L), eq(cursorTime), eq(5L), any(PageRequest.class));
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
