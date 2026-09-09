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
}
