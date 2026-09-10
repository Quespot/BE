package com.quespot.domain.mission.service;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.res.MissionArchiveItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.enums.ArchivePhotoSource;
import com.quespot.domain.mission.repository.ArchivePhotoRepository;
import com.quespot.domain.mission.repository.projection.ArchiveFeedRowProjection;
import com.quespot.global.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// GPS 인증 후 등록한 미션 사진 + 아카이브 자유 업로드 사진을 하나의 최신순
// 피드로 모아 보는 화면(#45 확장). 지도가 아니라 목록이라 좌표는 응답에 안
// 넣는다(mission_photos.latitude/longitude는 나중에 지도 뷰가 생길 때를
// 위해 컬럼은 그대로 둔다).
@Service
@RequiredArgsConstructor
public class MissionArchiveQueryService {

    private final ArchivePhotoRepository archivePhotoRepository;
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
}
