package com.quespot.domain.like.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.dto.res.MissionCourseListItemResponseDTO;

import java.util.List;

// 코스 카드는 코스 목록 응답과 같은 모양(지역·미션 수·보상·진행 상태)이라 그대로 재사용한다.
public record LikedCourseListResponseDTO(
        @Schema(description = "비면 빈 배열이다(null 아님)")
        List<MissionCourseListItemResponseDTO> items,
        int totalCount
) {
}
