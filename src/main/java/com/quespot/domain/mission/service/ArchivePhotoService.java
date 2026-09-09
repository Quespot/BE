package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.repository.ArchivePhotoRepository;
import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.service.S3ObjectKeyValidator;
import com.quespot.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArchivePhotoService {

    private final ArchivePhotoRepository archivePhotoRepository;
    private final S3ObjectKeyValidator s3ObjectKeyValidator;
    private final S3Service s3Service;

    @Transactional
    public ArchivePhoto registerPhoto(Long userId, String objectKey, String caption) {
        // archives/ 아래인지, 업로드한 본인이 맞는지 검증한다(#45 확장,
        // MissionPhotoService.registerPhoto와 동일 패턴 — S3 API 호출 없이
        // key 패턴만 검증).
        s3ObjectKeyValidator.validate(objectKey, userId, UploadPurpose.ARCHIVE);
        return archivePhotoRepository.save(ArchivePhoto.upload(userId, objectKey, caption));
    }

    // 저장된 objectKey를 매번 새로 서명한 presigned GET URL로 바꿔서 돌려준다
    // (버킷이 비공개라 저장된 값 그대로는 렌더링할 수 없다). presign은 로컬
    // 서명 계산이라 외부 API 호출이 아니다 — 트랜잭션 안에서 호출해도 된다.
    public String resolveViewUrl(ArchivePhoto photo) {
        return photo == null ? null : s3Service.createPresignedDownloadUrl(photo.getImageKey());
    }
}
