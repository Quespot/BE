package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.CourseMissionItemResultDTO;
import com.quespot.domain.mission.dto.MissionCourseDetailResultDTO;
import com.quespot.domain.mission.dto.MissionCourseListItemResultDTO;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MissionCourseQueryService {

    private final MissionCourseRepository missionCourseRepository;
    private final CourseMissionRepository courseMissionRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final CourseAttemptRepository courseAttemptRepository;
    private final CourseLockPolicy courseLockPolicy;

    @Transactional(readOnly = true)
    public List<MissionCourseListItemResultDTO> getCourses(Long userId) {
        List<MissionCourse> courses = missionCourseRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId);
        List<Long> courseIds = courses.stream().map(MissionCourse::getId).toList();

        // 사용자 생성 코스는 코스당 이 유저의 CourseAttempt가 최대 1건이라(비공개
        // 코스, 재시작 경로 없음), courseId → status 맵으로 한 번에 채운다.
        Map<Long, CourseAttemptStatus> statusByCourseId = new HashMap<>();
        courseAttemptRepository.findByCourseIdIn(courseIds)
                .forEach(ca -> statusByCourseId.put(ca.getCourse().getId(), ca.getStatus()));

        return courses.stream()
                .map(c -> new MissionCourseListItemResultDTO(c, statusByCourseId.get(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MissionCourseDetailResultDTO getCourseDetail(Long userId, Long courseId) {
        // 코스는 비공개(isPublic=false)라 소유자만 조회 가능하다. 존재 여부를
        // 노출하지 않기 위해 403이 아니라 404(COURSE_NOT_FOUND)로 통일한다.
        MissionCourse course = missionCourseRepository.findById(courseId)
                .filter(c -> c.getCreatedByUserId().equals(userId))
                .orElseThrow(() -> new MissionException(MissionErrorCode.COURSE_NOT_FOUND));

        List<CourseMission> courseMissions = courseMissionRepository.findByCourseIdOrderBySeq(courseId);
        List<Long> missionIds = courseMissions.stream().map(cm -> cm.getMission().getId()).toList();

        // 미션마다 따로 조회하지 않는다 — 한 번의 벌크 쿼리로 완료/진행중 missionId만
        // 뽑아 Set으로 만들고 CourseLockPolicy가 seq 순서대로 4상태를 계산한다.
        Set<Long> completedMissionIds = new HashSet<>();
        Set<Long> inProgressMissionIds = new HashSet<>();
        missionAttemptRepository
                .findByUserIdAndMissionIdInAndStatusIn(
                        userId, missionIds, List.of(MissionAttemptStatus.COMPLETED, MissionAttemptStatus.IN_PROGRESS)
                )
                .forEach(row -> {
                    if (row.getStatus() == MissionAttemptStatus.COMPLETED) {
                        completedMissionIds.add(row.getMissionId());
                    } else {
                        inProgressMissionIds.add(row.getMissionId());
                    }
                });

        List<CourseMissionItemResultDTO> missionItems =
                courseLockPolicy.resolveCourseMissionStatuses(courseMissions, completedMissionIds, inProgressMissionIds);

        CourseAttemptStatus myStatus = resolveMyStatus(userId, courseId);

        return new MissionCourseDetailResultDTO(course, missionItems, myStatus);
    }

    private CourseAttemptStatus resolveMyStatus(Long userId, Long courseId) {
        if (courseAttemptRepository.findByUserIdAndCourseIdAndStatus(userId, courseId, CourseAttemptStatus.COMPLETED).isPresent()) {
            return CourseAttemptStatus.COMPLETED;
        }
        if (courseAttemptRepository.findByUserIdAndCourseIdAndStatus(userId, courseId, CourseAttemptStatus.IN_PROGRESS).isPresent()) {
            return CourseAttemptStatus.IN_PROGRESS;
        }
        return null;
    }
}
