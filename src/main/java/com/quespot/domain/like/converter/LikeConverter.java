package com.quespot.domain.like.converter;

import com.quespot.domain.like.dto.LikedCourseRowDTO;
import com.quespot.domain.like.dto.LikedMissionRowDTO;
import com.quespot.domain.like.dto.res.LikedCourseListResponseDTO;
import com.quespot.domain.like.dto.res.LikedMissionListResponseDTO;
import com.quespot.domain.like.dto.res.LikedMissionResponseDTO;
import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.MissionCourseListItemResultDTO;
import com.quespot.domain.mission.dto.res.MissionCourseListItemResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.enums.CourseAttemptStatus;

import java.util.List;
import java.util.Map;

public class LikeConverter {

    private LikeConverter() {
    }

    public static LikedMissionResponseDTO toLikedMission(LikedMissionRowDTO row) {
        Mission mission = row.mission();
        return new LikedMissionResponseDTO(
                mission.getId(),
                mission.getTitle(),
                mission.getCategory(),
                mission.getSnapshotName(),
                mission.getSnapshotImageUrl(),
                mission.getRewardPoint(),
                mission.getEstimatedMinutes(),
                row.likedAt()
        );
    }

    public static LikedMissionListResponseDTO toLikedMissionList(List<LikedMissionRowDTO> rows) {
        List<LikedMissionResponseDTO> items = rows.stream().map(LikeConverter::toLikedMission).toList();
        return new LikedMissionListResponseDTO(items, items.size());
    }

    public static LikedCourseListResponseDTO toLikedCourseList(
            List<LikedCourseRowDTO> rows, Map<Long, CourseAttemptStatus> statusByCourseId
    ) {
        List<MissionCourseListItemResponseDTO> items = rows.stream()
                .map(row -> MissionConverter.toCourseListItem(
                        new MissionCourseListItemResultDTO(row.course(), statusByCourseId.get(row.course().getId()))
                ))
                .toList();
        return new LikedCourseListResponseDTO(items, items.size());
    }
}
