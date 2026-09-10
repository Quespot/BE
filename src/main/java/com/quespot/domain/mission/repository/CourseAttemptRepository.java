package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseAttemptRepository extends JpaRepository<CourseAttempt, Long> {

    Optional<CourseAttempt> findByUserIdAndCourseIdAndStatus(
            Long userId, Long courseId, CourseAttemptStatus status
    );

    List<CourseAttempt> findByUserIdAndStatus(Long userId, CourseAttemptStatus status);

    List<CourseAttempt> findByCourseIdIn(List<Long> courseIds);

    // 좋아요한 코스 목록의 내 진행 상태 채우기용(#50). 코스당 이 유저의 시도는 최대 1건.
    List<CourseAttempt> findByUserIdAndCourseIdIn(Long userId, List<Long> courseIds);
}
