package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionPhotoRepository;
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

        // 촬영 위치가 없으면 미션 스냅샷 좌표로 폴백한다 — 반경 500m 안에서
        // 자유롭게 찍는 정책이라 값이 없을 때 (0,0) 대신 스냅샷 좌표가 낫다.
        BigDecimal effectiveLatitude = latitude != null ? latitude : attempt.getMission().getSnapshotLatitude();
        BigDecimal effectiveLongitude = longitude != null ? longitude : attempt.getMission().getSnapshotLongitude();

        return missionPhotoRepository.save(
                MissionPhoto.record(attempt, imageUrl, caption, effectiveLatitude, effectiveLongitude, takenAt)
        );
    }

    @Transactional(readOnly = true)
    public Optional<MissionPhoto> findByAttemptId(Long attemptId) {
        return missionPhotoRepository.findByAttemptId(attemptId);
    }
}
