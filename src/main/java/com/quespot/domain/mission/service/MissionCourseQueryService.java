package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.CourseMissionItemResultDTO;
import com.quespot.domain.mission.dto.MissionCourseDetailResultDTO;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MissionCourseQueryService {

    private final MissionCourseRepository missionCourseRepository;
    private final CourseMissionRepository courseMissionRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final CourseAttemptRepository courseAttemptRepository;

    @Transactional(readOnly = true)
    public List<MissionCourse> getCourses(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return missionCourseRepository.findByStatus(MissionCourseStatus.ACTIVE);
        }
        return missionCourseRepository.findByRegionCodeAndStatus(regionCode, MissionCourseStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public MissionCourseDetailResultDTO getCourseDetail(Long userId, Long courseId) {
        MissionCourse course = missionCourseRepository.findById(courseId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.COURSE_NOT_FOUND));

        List<CourseMission> courseMissions = courseMissionRepository.findByCourseIdOrderBySeq(courseId);
        List<Long> missionIds = courseMissions.stream().map(cm -> cm.getMission().getId()).toList();

        // 미션마다 따로 조회하지 않는다 — 한 번의 벌크 쿼리로 완료된 missionId만
        // 뽑아 Set으로 만들고 체크리스트를 O(1) 조회로 채운다.
        Set<Long> completedMissionIds = new HashSet<>();
        missionAttemptRepository
                .findByUserIdAndMissionIdInAndStatusIn(userId, missionIds, List.of(MissionAttemptStatus.COMPLETED))
                .forEach(row -> completedMissionIds.add(row.getMissionId()));

        List<CourseMissionItemResultDTO> missionItems = courseMissions.stream()
                .map(cm -> new CourseMissionItemResultDTO(cm, completedMissionIds.contains(cm.getMission().getId())))
                .toList();

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
