package com.quespot.domain.mission.dto;

import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.CourseAttemptStatus;

import java.util.List;

public record MissionCourseDetailResultDTO(
        MissionCourse course,
        List<CourseMissionItemResultDTO> missions,
        CourseAttemptStatus myStatus
) {
}
