package com.quespot.domain.mission.service;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.res.MissionArchiveItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveMapResponseDTO;
import com.quespot.domain.mission.enums.ArchivePhotoSource;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.repository.ArchivePhotoRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.projection.ArchiveFeedRowProjection;
import com.quespot.global.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionArchiveQueryService {

    private final ArchivePhotoRepository archivePhotoRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final ArchiveCursorCodec archiveCursorCodec;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public MissionArchiveListResponseDTO getArchives(Long userId, String cursorValue, int size) {
        ArchiveCursor cursor = (cursorValue == null || cursorValue.isBlank())
                ? null
                : archiveCursorCodec.decode(cursorValue);
        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        String cursorSource = cursor == null ? null : cursor.source().name();
        Long cursorId = cursor == null ? null : cursor.photoId();

        List<ArchiveFeedRowProjection> rows = archivePhotoRepository.findFeedPage(
                userId, cursorCreatedAt, cursorSource, cursorId, size + 1
        );
        boolean hasNext = rows.size() > size;
        List<ArchiveFeedRowProjection> page = hasNext ? rows.subList(0, size) : rows;

        String nextCursor = hasNext
                ? archiveCursorCodec.encode(new ArchiveCursor(
                        page.get(page.size() - 1).getCreatedAt(),
                        ArchivePhotoSource.valueOf(page.get(page.size() - 1).getSource()),
                        page.get(page.size() - 1).getId()
                ))
                : null;

        // 저장된 값은 objectKey라 매번 새로 서명한 presigned GET URL로 바꿔서
        // 돌려준다(버킷 비공개, #45). presign은 로컬 서명 계산이라 사진 수만큼
        // 반복해도 외부 API 호출이 아니다.
        List<MissionArchiveItemResponseDTO> items = page.stream()
                .map(row -> MissionConverter.toArchiveItem(
                        row, fileService.createPresignedDownloadUrl(row.getImageKey())
                ))
                .toList();

        return MissionConverter.toArchiveListResponse(items, nextCursor, hasNext);
    }

    // 지도형 아카이브 조회 로직
    @Transactional(readOnly = true)
    public MissionArchiveMapResponseDTO getMapArchive(
            Long userId,
            String yearMonthValue,
            String regionCode,
            MissionCategory category
    ) {
        YearMonth yearMonth = yearMonthValue == null ? null : YearMonth.parse(yearMonthValue);
        LocalDateTime startAt = yearMonth == null ? null : yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = yearMonth == null ? null : yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        var completedMissions = missionAttemptRepository.findCompletedMissionArchive(
                        userId,
                        startAt,
                        endAt,
                        regionCode,
                        category == null ? null : category.name()
                ).stream()
                .map(MissionConverter::toCompletedMissionArchiveItem)
                .toList();

        return MissionConverter.toArchiveMapResponse(completedMissions);
    }
}
