package com.quespot.domain.like.service;

import com.quespot.domain.like.enums.LikeTargetType;
import com.quespot.domain.like.repository.LikeRepository;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import com.quespot.domain.mission.service.CourseLockPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 등록은 리소스마다 검증이 다르고(잠긴 미션, 남의 코스) 저장은 공통 upsert 하나다.
// 해제는 검증 없이 항상 성공 — 대상이 나중에 비활성으로 바뀌어도 사용자는
// 좋아요를 풀 수 있어야 한다.
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final MissionRepository missionRepository;
    private final MissionCourseRepository missionCourseRepository;
    private final CourseLockPolicy courseLockPolicy;

    @Transactional
    public void likeMission(Long userId, Long missionId) {
        missionRepository.findByIdAndStatus(missionId, MissionStatus.ACTIVE)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));
        // 미션 시작과 같은 잠금 규칙(CourseLockPolicy가 유일한 진실, #39).
        if (courseLockPolicy.resolveLockedMissionIds(userId, List.of(missionId)).contains(missionId)) {
            throw new MissionException(MissionErrorCode.MISSION_LOCKED);
        }
        likeRepository.upsert(userId, LikeTargetType.MISSION.name(), missionId);
    }

    @Transactional
    public void likeCourse(Long userId, Long courseId) {
        // 코스는 비공개라 소유자만 접근 가능 — 코스 상세와 같이 존재를 숨기려고 404.
        missionCourseRepository.findByIdAndStatus(courseId, MissionCourseStatus.ACTIVE)
                .filter(course -> course.getCreatedByUserId().equals(userId))
                .orElseThrow(() -> new MissionException(MissionErrorCode.COURSE_NOT_FOUND));
        likeRepository.upsert(userId, LikeTargetType.COURSE.name(), courseId);
    }

    @Transactional
    public void unlike(Long userId, LikeTargetType targetType, Long targetId) {
        likeRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }
}
