package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionCourseRepository;
import com.quespot.domain.reward.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseAttemptService {

    private final CourseAttemptRepository courseAttemptRepository;
    private final MissionCourseRepository missionCourseRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final CourseMissionRepository courseMissionRepository;
    private final PointService pointService;

    @Transactional
    public CourseAttempt start(Long userId, Long courseId) {
        MissionCourse course = missionCourseRepository.findById(courseId)
                .orElseThrow(() -> new MissionException(MissionErrorCode.COURSE_NOT_FOUND));

        return courseAttemptRepository
                .findByUserIdAndCourseIdAndStatus(userId, courseId, CourseAttemptStatus.IN_PROGRESS)
                .orElseGet(() -> createNewAttempt(userId, course));
    }

    private CourseAttempt createNewAttempt(Long userId, MissionCourse course) {
        boolean alreadyCompleted = courseAttemptRepository
                .findByUserIdAndCourseIdAndStatus(userId, course.getId(), CourseAttemptStatus.COMPLETED)
                .isPresent();
        if (alreadyCompleted) {
            throw new MissionException(MissionErrorCode.COURSE_ALREADY_COMPLETED);
        }

        return courseAttemptRepository.save(CourseAttempt.start(userId, course));
    }

    @Transactional(readOnly = true)
    public List<CourseAttempt> getInProgressList(Long userId) {
        return courseAttemptRepository.findByUserIdAndStatus(userId, CourseAttemptStatus.IN_PROGRESS);
    }

    // 코스를 포기해도 개별 미션 시도는 QUIT하지 않는다 — courseAttemptId만
    // 비워서 단독 수행으로 전환한다(#39 정책).
    @Transactional
    public void quit(Long userId, Long courseAttemptId) {
        CourseAttempt courseAttempt = courseAttemptRepository.findById(courseAttemptId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new MissionException(MissionErrorCode.COURSE_ATTEMPT_NOT_FOUND));
        if (courseAttempt.getStatus() != CourseAttemptStatus.IN_PROGRESS) {
            throw new MissionException(MissionErrorCode.COURSE_ATTEMPT_NOT_IN_PROGRESS);
        }

        courseAttempt.quit();
        for (MissionAttempt missionAttempt : missionAttemptRepository.findByCourseAttemptId(courseAttemptId)) {
            missionAttempt.clearCourseAttempt();
        }
    }
}
