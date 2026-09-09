package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionPhotoRepository;
import com.quespot.global.s3.enums.UploadPurpose;
import com.quespot.global.s3.service.S3ImageUrlValidator;
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
    private final S3ImageUrlValidator s3ImageUrlValidator;

    @Transactional
    public MissionPhoto registerPhoto(
            Long userId,
            Long attemptId,
            String imageUrl,
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

        // 우리 버킷 URL인지, missions/ 아래인지, 업로드한 본인이 맞는지 검증한다.
        // S3 API 호출(headObject)은 하지 않는다 — URL 패턴 검증만(#45 결정 사항).
        s3ImageUrlValidator.validate(imageUrl, userId, UploadPurpose.MISSION);

        return missionPhotoRepository.save(
                MissionPhoto.record(attempt, imageUrl, caption, effectiveLatitude, effectiveLongitude, takenAt)
        );
    }

    @Transactional(readOnly = true)
    public Optional<MissionPhoto> findByAttemptId(Long attemptId) {
        return missionPhotoRepository.findByAttemptId(attemptId);
    }
}
