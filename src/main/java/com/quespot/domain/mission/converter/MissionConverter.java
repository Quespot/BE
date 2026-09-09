package com.quespot.domain.mission.converter;

import com.quespot.domain.mission.dto.ArrivalResultDTO;
import com.quespot.domain.mission.dto.res.ArrivalResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResultResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateResponseDTO;
import com.quespot.domain.mission.dto.res.MissionDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionPhotoResponseDTO;
import com.quespot.domain.mission.dto.res.VerificationGuideResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import com.quespot.domain.mission.service.MissionArrivalService;
import com.quespot.domain.spot.entity.Spot;
import org.springframework.data.domain.Page;

import java.util.List;

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

    public static MissionAttemptResponseDTO toAttemptResponse(MissionAttempt attempt) {
        return new MissionAttemptResponseDTO(
                attempt.getId(),
                attempt.getMission().getId(),
                attempt.getMission().getTitle(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getEarnedPoint()
        );
    }

    public static MissionAttemptListResponseDTO toAttemptListResponse(List<MissionAttempt> attempts) {
        return new MissionAttemptListResponseDTO(
                attempts.stream().map(MissionConverter::toAttemptResponse).toList()
        );
    }

    public static ArrivalResponseDTO toArrivalResponse(ArrivalResultDTO result) {
        return new ArrivalResponseDTO(
                result.success(), result.distanceMeters(), result.radiusMeters(),
                result.status(), result.earnedPoint()
        );
    }

    public static VerificationGuideResponseDTO toVerificationGuideResponse(MissionAttempt attempt) {
        return new VerificationGuideResponseDTO(
                attempt.getId(),
                attempt.getMission().getSnapshotLatitude(),
                attempt.getMission().getSnapshotLongitude(),
                MissionArrivalService.RADIUS_METERS
        );
    }

    public static MissionAttemptResultResponseDTO toAttemptResultResponse(MissionAttempt attempt, MissionPhoto photo) {
        return new MissionAttemptResultResponseDTO(
                attempt.getId(),
                attempt.getMission().getTitle(),
                attempt.getEarnedPoint(),
                attempt.getCompletedAt(),
                photo == null ? null : photo.getImageUrl()
        );
    }

    public static MissionPhotoResponseDTO toPhotoResponse(MissionPhoto photo) {
        return new MissionPhotoResponseDTO(
                photo.getId(), photo.getImageUrl(), photo.getCaption(),
                photo.getLatitude(), photo.getLongitude(), photo.getTakenAt()
        );
    }

    private static String preferredImage(Spot spot) {
        return hasText(spot.getImageUrl()) ? spot.getImageUrl() : spot.getThumbnailUrl();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
