package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.CourseMissionItemResultDTO;
import com.quespot.domain.mission.dto.MissionCourseDetailResultDTO;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCourseCandidateRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.mission.repository.projection.CourseCandidateMissionProjection;
import com.quespot.domain.mission.repository.projection.ExistingCoursePairProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 사용자가 anchor 미션 하나를 고르면 규칙 기반으로 코스를 생성하고 즉시 시작한다
// (#39 재설계). 반경 500m→1km 순회, 페어 단위 재시도로 기존 코스와의 중복을
// 피한다(스펙 2번 섹션).
@Service
@RequiredArgsConstructor
public class MissionCourseGenerationService {

    private static final int RADIUS_STEP1_METERS = 500;
    private static final int RADIUS_STEP2_METERS = 1000;
    private static final int CANDIDATE_FETCH_LIMIT = 20;
    private static final float BONUS_RATE = 0.30f;
    private static final List<String> SECOND_CATEGORIES =
            List.of(MissionCategory.NATURE.name(), MissionCategory.CULTURE.name());
    private static final List<String> THIRD_CATEGORIES =
            List.of(MissionCategory.CULTURE.name(), MissionCategory.NIGHT_VIEW.name());

    private final MissionCourseCandidateRepository candidateRepository;
    private final MissionCourseRepository missionCourseRepository;
    private final CourseMissionRepository courseMissionRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final CourseAttemptService courseAttemptService;
    private final CourseLockPolicy courseLockPolicy;

    @Transactional
    public MissionCourseDetailResultDTO generateAndStart(Long userId, Long anchorMissionId) {
        Mission anchor = candidateRepository.findById(anchorMissionId)
                .filter(m -> m.getStatus() == MissionStatus.ACTIVE)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        boolean anchorAttempted = !missionAttemptRepository
                .findByUserIdAndMissionIdInAndStatusIn(
                        userId, List.of(anchorMissionId),
                        List.of(MissionAttemptStatus.IN_PROGRESS, MissionAttemptStatus.COMPLETED)
                )
                .isEmpty();
        if (anchorAttempted) {
            throw new MissionException(MissionErrorCode.ANCHOR_NOT_AVAILABLE);
        }

        Set<String> rejectedPairs = new HashSet<>();
        for (ExistingCoursePairProjection p : courseMissionRepository.findExistingPairs(userId, anchor.getId())) {
            rejectedPairs.add(p.getMission2Id() + ":" + p.getMission3Id());
        }

        CandidatePair pair = null;
        for (int radius : List.of(RADIUS_STEP1_METERS, RADIUS_STEP2_METERS)) {
            pair = tryFindPair(userId, anchor, radius, rejectedPairs);
            if (pair != null) {
                break;
            }
        }
        if (pair == null) {
            throw new MissionException(MissionErrorCode.COURSE_GENERATION_FAILED);
        }

        Mission m2 = candidateRepository.findById(pair.mission2Id())
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));
        Mission m3 = candidateRepository.findById(pair.mission3Id())
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        int totalRewardPoint = anchor.getRewardPoint() + m2.getRewardPoint() + m3.getRewardPoint();
        int bonusPoint = Math.round(totalRewardPoint * BONUS_RATE);
        int estimatedMinutes = anchor.getEstimatedMinutes() + m2.getEstimatedMinutes() + m3.getEstimatedMinutes();
        String cleanedAnchor = PlaceNameFormatter.clean(anchor.getSnapshotName());
        String cleanedM2 = PlaceNameFormatter.clean(m2.getSnapshotName());
        String cleanedM3 = PlaceNameFormatter.clean(m3.getSnapshotName());
        String name = cleanedAnchor + " 주변 코스";
        String description = "%s에서 시작해 %s, %s을 걷는 코스예요.\n약 %d분, 완주하면 %dP를 더 받아요."
                .formatted(cleanedAnchor, cleanedM2, cleanedM3, estimatedMinutes, bonusPoint);
        String regionCode = anchor.getSpot().getLdongSignguCd();

        MissionCourse course = missionCourseRepository.save(MissionCourse.generate(
                anchor, userId, 3, name, description, anchor.getSnapshotImageUrl(), regionCode,
                totalRewardPoint, bonusPoint, estimatedMinutes
        ));

        List<Mission> ordered = List.of(anchor, m2, m3);
        List<CourseMission> courseMissions = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            courseMissions.add(courseMissionRepository.save(CourseMission.of(course, ordered.get(i), i + 1)));
        }

        courseAttemptService.start(userId, course.getId());

        // 방금 생성한 코스라 완료/진행중 미션이 있을 수 없다 — CourseLockPolicy를
        // 재사용해 잠금 규칙이 세 사용처에서 계속 하나로 유지되게 한다.
        List<CourseMissionItemResultDTO> missionItems =
                courseLockPolicy.resolveCourseMissionStatuses(courseMissions, Set.of(), Set.of());
        return new MissionCourseDetailResultDTO(course, missionItems, CourseAttemptStatus.IN_PROGRESS);
    }

    private CandidatePair tryFindPair(Long userId, Mission anchor, int radiusMeters, Set<String> rejectedPairs) {
        List<CourseCandidateMissionProjection> m2Candidates = candidateRepository.findNearestCandidates(
                anchor.getSnapshotLatitude(), anchor.getSnapshotLongitude(), radiusMeters,
                SECOND_CATEGORIES, List.of(anchor.getId()), userId, CANDIDATE_FETCH_LIMIT
        );
        for (CourseCandidateMissionProjection m2 : m2Candidates) {
            List<CourseCandidateMissionProjection> m3Candidates = candidateRepository.findNearestCandidates(
                    m2.getLatitude(), m2.getLongitude(), radiusMeters,
                    THIRD_CATEGORIES, List.of(anchor.getId(), m2.getMissionId()), userId, CANDIDATE_FETCH_LIMIT
            );
            for (CourseCandidateMissionProjection m3 : m3Candidates) {
                String key = m2.getMissionId() + ":" + m3.getMissionId();
                if (!rejectedPairs.contains(key)) {
                    return new CandidatePair(m2.getMissionId(), m3.getMissionId());
                }
            }
        }
        return null;
    }

    private record CandidatePair(Long mission2Id, Long mission3Id) {
    }
}
