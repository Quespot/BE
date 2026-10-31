package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.CourseAttemptStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class CourseAttemptTest {

    private MissionCourse course() throws Exception {
        MissionCourse course = new MissionCourse();
        setField(course, "id", 1L);
        setField(course, "bonusPoint", 50);
        setField(course, "name", "정동 도보 코스");
        return course;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = MissionCourse.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void startCreatesInProgressCourseAttempt() throws Exception {
        MissionCourse course = course();

        CourseAttempt attempt = CourseAttempt.start(1L, course);

        assertThat(attempt.getUserId()).isEqualTo(1L);
        assertThat(attempt.getCourse()).isEqualTo(course);
        assertThat(attempt.getStatus()).isEqualTo(CourseAttemptStatus.IN_PROGRESS);
        assertThat(attempt.getStartedAt()).isNotNull();
        assertThat(attempt.getCompletedAt()).isNull();
    }

    @Test
    void completeSetsCompletedStatusAndBonusPoint() throws Exception {
        CourseAttempt attempt = CourseAttempt.start(1L, course());

        attempt.complete(50);

        assertThat(attempt.getStatus()).isEqualTo(CourseAttemptStatus.COMPLETED);
        assertThat(attempt.getEarnedBonusPoint()).isEqualTo(50);
        assertThat(attempt.getCompletedAt()).isNotNull();
    }

    @Test
    void quitSetsQuitStatus() throws Exception {
        CourseAttempt attempt = CourseAttempt.start(1L, course());

        attempt.quit();

        assertThat(attempt.getStatus()).isEqualTo(CourseAttemptStatus.QUIT);
    }
}
