package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionPhotoRepository;
import com.quespot.global.file.enums.UploadPurpose;
import com.quespot.global.file.service.FileService;
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
    private final FileService fileService;

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

        fileService.validateOwnedObjectKey(userId, UploadPurpose.MISSION, objectKey);

        return missionPhotoRepository.save(
                MissionPhoto.record(attempt, objectKey, caption, effectiveLatitude, effectiveLongitude, takenAt)
        );
    }

    @Transactional(readOnly = true)
    public Optional<MissionPhoto> findByAttemptId(Long attemptId) {
        return missionPhotoRepository.findByAttemptId(attemptId);
    }

    // 저장된 objectKey를 매번 새로 서명한 presigned GET URL로 바꿔서 돌려준다
    // (버킷이 비공개라 저장된 값 그대로는 렌더링할 수 없다, #45). presign은
    // 로컬 서명 계산이라 외부 API 호출이 아니다 — 트랜잭션 안에서 호출해도 된다.
    public String resolveViewUrl(MissionPhoto photo) {
        return photo == null ? null : fileService.createPresignedDownloadUrl(photo.getImageKey());
    }
}
