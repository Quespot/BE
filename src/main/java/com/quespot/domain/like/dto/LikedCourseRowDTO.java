package com.quespot.domain.like.dto;

import com.quespot.domain.mission.entity.MissionCourse;

import java.time.LocalDateTime;

// LikeRepository.findLikedCourses의 JPQL 생성자 표현식 결과. 서비스 내부용.
public record LikedCourseRowDTO(MissionCourse course, LocalDateTime likedAt) {
}
