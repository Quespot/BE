package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.repository.ArchivePhotoRepository;
import com.quespot.domain.reward.service.AchievementService;
import com.quespot.global.file.enums.UploadPurpose;
import com.quespot.global.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArchivePhotoService {

    private final ArchivePhotoRepository archivePhotoRepository;
    private final FileService fileService;
    private final AchievementService achievementService;

    @Transactional
    public ArchivePhoto registerPhoto(Long userId, String objectKey, String caption) {
        fileService.validateOwnedObjectKey(userId, UploadPurpose.ARCHIVE, objectKey);

        // 같은 objectKey로 재제출(클라이언트 재시도 등)되면 새 행을 또 만들지
        // 않고 기존 행을 그대로 돌려준다 — 좋아요/저장과 같은 멱등 정책(CLAUDE.md).
        // (user_id, image_key) UNIQUE 제약이 동시 요청 경합까지 DB 레벨에서 막는다.
        Optional<ArchivePhoto> existing = archivePhotoRepository.findByUserIdAndImageKey(userId, objectKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        ArchivePhoto saved = archivePhotoRepository.save(ArchivePhoto.upload(userId, objectKey, caption));
        // 새로 저장했을 때만 사진작가 배지 판정 — 재제출은 사진 수가 안 늘어난다(#50).
        achievementService.onPhotoRegistered(userId);
        return saved;
    }

    // 저장된 objectKey를 매번 새로 서명한 presigned GET URL로 바꿔서 돌려준다
    // (버킷이 비공개라 저장된 값 그대로는 렌더링할 수 없다). presign은 로컬
    // 서명 계산이라 외부 API 호출이 아니다 — 트랜잭션 안에서 호출해도 된다.
    public String resolveViewUrl(ArchivePhoto photo) {
        return photo == null ? null : fileService.createPresignedDownloadUrl(photo.getImageKey());
    }
}
