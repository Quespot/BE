package com.quespot.domain.mission.service;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.repository.MissionPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// GPS 인증 후 등록한 미션 사진들을 최신순으로 모아 보는 화면(#45). 지도가
// 아니라 목록이라 좌표는 응답에 안 넣는다(mission_photos.latitude/longitude는
// 나중에 지도 뷰가 생길 때를 위해 컬럼은 그대로 둔다).
@Service
@RequiredArgsConstructor
public class MissionArchiveQueryService {

    private final MissionPhotoRepository missionPhotoRepository;
    private final ArchiveCursorCodec archiveCursorCodec;

    @Transactional(readOnly = true)
    public MissionArchiveListResponseDTO getArchives(Long userId, String cursorValue, int size) {
        ArchiveCursor cursor = (cursorValue == null || cursorValue.isBlank())
                ? null
                : archiveCursorCodec.decode(cursorValue);
        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        Long cursorId = cursor == null ? null : cursor.photoId();

        List<MissionPhoto> rows = missionPhotoRepository.findArchivePage(
                userId, cursorCreatedAt, cursorId, PageRequest.of(0, size + 1)
        );
        boolean hasNext = rows.size() > size;
        List<MissionPhoto> page = hasNext ? rows.subList(0, size) : rows;

        String nextCursor = hasNext
                ? archiveCursorCodec.encode(new ArchiveCursor(
                        page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId()
                ))
                : null;

        return MissionConverter.toArchiveListResponse(page, nextCursor, hasNext);
    }
}
