package com.quespot.domain.like.dto;

import com.quespot.domain.mission.entity.Mission;

import java.time.LocalDateTime;

// LikeRepository.findLikedMissions의 JPQL 생성자 표현식 결과. 서비스 내부용.
public record LikedMissionRowDTO(Mission mission, LocalDateTime likedAt) {
}
