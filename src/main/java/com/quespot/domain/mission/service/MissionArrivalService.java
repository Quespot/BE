package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.ArrivalResultDTO;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.reward.service.AchievementService;
import com.quespot.domain.reward.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MissionArrivalService {

    public static final int RADIUS_METERS = 500;

    private final MissionAttemptRepository missionAttemptRepository;
    private final PointService pointService;
    private final GeoDistanceCalculator geoDistanceCalculator;
    private final CourseAttemptService courseAttemptService;
    private final AchievementService achievementService;

    // 완료 처리와 포인트 지급을 하나의 트랜잭션(기본 REQUIRED)으로 묶는다.
    // REQUIRES_NEW를 쓰면 안 된다 — "완료됐는데 포인트 없음"이나 "포인트는
    // 나갔는데 미션은 여전히 진행 중"은 둘 다 있어선 안 되는 상태다.
    // pointService.credit(...)도 REQUIRES_NEW가 아니므로 이 트랜잭션에
    // 그대로 합류한다.
    // courseAttemptService.tryCompleteViaMissionCompletion(...)도 REQUIRES_NEW가
    // 아니라 이 트랜잭션에 합류한다 — 미션 완료+보상+코스 완주+보너스가 전부
    // 하나의 사건이다(#39).
    // achievementService.onMissionCompleted(...)도 같은 트랜잭션에 합류한다 — 코스
    // 완주 판정 뒤에 호출해야 COURSE_COMPLETED 배지가 즉시 반영된다(#50).
    @Transactional
    public ArrivalResultDTO arrive(Long userId, Long attemptId, BigDecimal latitude, BigDecimal longitude) {
        MissionAttempt attempt = missionAttemptRepository.findById(attemptId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new MissionException(MissionErrorCode.ATTEMPT_NOT_FOUND));

        if (attempt.getStatus() == MissionAttemptStatus.COMPLETED) {
            return successResult(attempt, 0);
        }
        if (attempt.getStatus() == MissionAttemptStatus.QUIT) {
            throw new MissionException(MissionErrorCode.ATTEMPT_QUIT);
        }

        long distanceMeters = geoDistanceCalculator.distanceMeters(
                latitude, longitude,
                attempt.getMission().getSnapshotLatitude(), attempt.getMission().getSnapshotLongitude()
        );

        if (distanceMeters > RADIUS_METERS) {
            return new ArrivalResultDTO(false, distanceMeters, RADIUS_METERS, attempt.getStatus(), null);
        }

        Integer earnedPoint = attempt.getMission().getRewardPoint();
        // 지역 코드는 credit() 전에 읽어둔다 — credit()의 user_points upsert가
        // @Modifying(clearAutomatically = true)라 영속성 컨텍스트를 비우고, 그 뒤엔
        // attempt.mission.spot 지연 로딩이 LazyInitializationException을 낸다.
        // Mission은 지역을 스냅샷하지 않으므로 spot을 타야 한다(#50).
        String regionCode = attempt.getMission().getSpot().getLdongRegnCd();
        attempt.complete(latitude, longitude, earnedPoint);
        pointService.credit(
                userId, earnedPoint, "MISSION_REWARD", "MISSION_ATTEMPT", attempt.getId(),
                attempt.getMission().getTitle() + " 완료 보상"
        );

        if (attempt.getCourseAttemptId() != null) {
            courseAttemptService.tryCompleteViaMissionCompletion(attempt.getCourseAttemptId());
        }

        achievementService.onMissionCompleted(userId, regionCode);

        return successResult(attempt, distanceMeters);
    }

    private ArrivalResultDTO successResult(MissionAttempt attempt, long distanceMeters) {
        return new ArrivalResultDTO(true, distanceMeters, RADIUS_METERS, attempt.getStatus(), attempt.getEarnedPoint());
    }
}
