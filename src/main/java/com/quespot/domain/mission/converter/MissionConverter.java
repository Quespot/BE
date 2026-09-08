package com.quespot.domain.mission.converter;

import com.quespot.domain.mission.dto.res.MissionCandidateListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateResponseDTO;
import com.quespot.domain.mission.dto.res.MissionDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListItemResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import com.quespot.domain.spot.entity.Spot;
import org.springframework.data.domain.Page;

public class MissionConverter {

    private MissionConverter() {
    }

    public static MissionCandidateResponseDTO toCandidateResponse(MissionCandidate candidate) {
        Spot spot = candidate.getSpot();

        return new MissionCandidateResponseDTO(
                candidate.getId(),
                spot.getId(),
                spot.getName(),
                spot.getAddress(),
                spot.getLatitude(),
                spot.getLongitude(),
                preferredImage(spot),
                candidate.getTemplateCode(),
                candidate.getGeneratorVersion(),
                candidate.getGeneratedTitle(),
                candidate.getGeneratedDescription(),
                candidate.getSuggestedCategory(),
                candidate.getSuggestedRewardPoint(),
                candidate.getSuggestedEstimatedMinutes(),
                candidate.getStatus(),
                candidate.getReason(),
                candidate.getReviewedBy() == null ? null : candidate.getReviewedBy().getId(),
                candidate.getReviewedAt(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }

    public static MissionCandidateListResponseDTO toCandidateListResponse(Page<MissionCandidate> page) {
        return new MissionCandidateListResponseDTO(
                page.getContent().stream().map(MissionConverter::toCandidateResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public static MissionListItemResponseDTO toMissionListItem(
            MissionListProjection projection,
            UserMissionStatus userMissionStatus,
            boolean includeDistance
    ) {
        return new MissionListItemResponseDTO(
                projection.getMissionId(),
                projection.getTitle(),
                MissionCategory.valueOf(projection.getCategory()),
                projection.getSpotName(),
                projection.getAddress(),
                projection.getImageUrl(),
                includeDistance ? Math.round(projection.getSortValue()) : null,
                projection.getRewardPoint(),
                projection.getEstimatedMinutes(),
                userMissionStatus,
                userMissionStatus.canStart()
        );
    }

    public static MissionDetailResponseDTO toMissionDetail(
            Mission mission,
            Long distanceMeters,
            UserMissionStatus userMissionStatus
    ) {
        return new MissionDetailResponseDTO(
                mission.getId(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getCategory(),
                mission.getSnapshotName(),
                mission.getSnapshotAddress(),
                mission.getSnapshotLatitude(),
                mission.getSnapshotLongitude(),
                mission.getSnapshotImageUrl(),
                distanceMeters,
                mission.getRewardPoint(),
                mission.getEstimatedMinutes(),
                userMissionStatus,
                userMissionStatus.canStart(),
                false,
                userMissionStatus.canCreateArchive()
        );
    }

    private static String preferredImage(Spot spot) {
        return hasText(spot.getImageUrl()) ? spot.getImageUrl() : spot.getThumbnailUrl();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
