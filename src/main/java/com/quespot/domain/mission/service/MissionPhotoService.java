package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionPhotoRepository;
import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.service.S3ObjectKeyValidator;
import com.quespot.domain.reward.service.AchievementService;
import com.quespot.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MissionPhotoService {

    private final MissionAttemptRepository missionAttemptRepository;
    private final MissionPhotoRepository missionPhotoRepository;
    private final S3ObjectKeyValidator s3ObjectKeyValidator;
    private final S3Service s3Service;
    private final AchievementService achievementService;

    @Transactional
    public MissionPhoto registerPhoto(
            Long userId,
            Long attemptId,
            String objectKey,
            String caption,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime takenAt
    ) {
        MissionAttempt attempt = missionAttemptRepository.findById(attemptId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new MissionException(MissionErrorCode.ATTEMPT_NOT_FOUND));

        if (attempt.getStatus() != MissionAttemptStatus.COMPLETED) {
            throw new MissionException(MissionErrorCode.PHOTO_ATTEMPT_NOT_COMPLETED);
        }
        if (missionPhotoRepository.existsByAttemptId(attemptId)) {
            throw new MissionException(MissionErrorCode.PHOTO_ALREADY_EXISTS);
        }
        // 위도/경도는 둘 다 오거나 둘 다 없어야 한다 — 하나만 오면 제출 좌표와
        // 스냅샷 좌표가 뒤섞인 무의미한 좌표쌍이 저장된다.
        if ((latitude == null) != (longitude == null)) {
            throw new MissionException(MissionErrorCode.INVALID_LOCATION);
        }

        // 촬영 위치가 없으면 미션 스냅샷 좌표로 폴백한다 — 반경 500m 안에서
        // 자유롭게 찍는 정책이라 값이 없을 때 (0,0) 대신 스냅샷 좌표가 낫다.
        BigDecimal effectiveLatitude = latitude != null ? latitude : attempt.getMission().getSnapshotLatitude();
        BigDecimal effectiveLongitude = longitude != null ? longitude : attempt.getMission().getSnapshotLongitude();

        // missions/ 아래인지, 업로드한 본인이 맞는지 검증한다. S3 API 호출
        // (headObject 등)은 하지 않는다 — key 패턴 검증만(#45 결정 사항).
        s3ObjectKeyValidator.validate(objectKey, userId, UploadPurpose.MISSION);

        MissionPhoto photo = missionPhotoRepository.save(
                MissionPhoto.record(attempt, objectKey, caption, effectiveLatitude, effectiveLongitude, takenAt)
        );
        // 사진작가 배지 판정(#50) — 같은 트랜잭션에 합류한다.
        achievementService.onPhotoRegistered(userId);
        return photo;
    }

    @Transactional(readOnly = true)
    public Optional<MissionPhoto> findByAttemptId(Long attemptId) {
        return missionPhotoRepository.findByAttemptId(attemptId);
    }

    // 저장된 objectKey를 매번 새로 서명한 presigned GET URL로 바꿔서 돌려준다
    // (버킷이 비공개라 저장된 값 그대로는 렌더링할 수 없다, #45). presign은
    // 로컬 서명 계산이라 외부 API 호출이 아니다 — 트랜잭션 안에서 호출해도 된다.
    public String resolveViewUrl(MissionPhoto photo) {
        return photo == null ? null : s3Service.createPresignedDownloadUrl(photo.getImageKey());
    }
}
