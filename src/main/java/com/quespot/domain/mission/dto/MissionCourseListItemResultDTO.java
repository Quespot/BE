package com.quespot.domain.mission.dto;

import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;

public record MissionCourseListItemResultDTO(
        MissionCourse course,
        CourseAttemptStatus myStatus
) {
}
