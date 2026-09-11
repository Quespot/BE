package com.quespot.domain.mission.converter;

import com.quespot.domain.mission.dto.ArrivalResultDTO;
import com.quespot.domain.mission.dto.res.ArrivalResponseDTO;
import com.quespot.domain.mission.dto.res.CompletedMissionArchiveItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResultResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveFootprintResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveMapResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateResponseDTO;
import com.quespot.domain.mission.dto.res.MissionDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionPhotoResponseDTO;
import com.quespot.domain.mission.dto.res.VerificationGuideResponseDTO;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.entity.MissionPhoto;
import com.quespot.domain.mission.enums.ArchivePhotoSource;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.projection.ArchiveFeedRowProjection;
import com.quespot.domain.mission.repository.projection.CompletedMissionArchiveProjection;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import com.quespot.domain.mission.service.MissionArrivalService;
import com.quespot.domain.spot.entity.Spot;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;

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

    // photoViewUrl은 서비스 레이어가 MissionPhotoService.resolveViewUrl(photo)로
    // 미리 만들어 넘긴다 — MissionConverter는 static 유틸이라 FileService를 주입받을
    // 수 없다(엔티티→DTO 변환은 static Converter로 한다는 컨벤션 유지, #45).
    public static MissionAttemptResultResponseDTO toAttemptResultResponse(MissionAttempt attempt, String photoViewUrl) {
        return new MissionAttemptResultResponseDTO(
                attempt.getId(),
                attempt.getMission().getTitle(),
                attempt.getEarnedPoint(),
                attempt.getCompletedAt(),
                photoViewUrl
        );
    }

    public static MissionPhotoResponseDTO toPhotoResponse(MissionPhoto photo, String photoViewUrl) {
        return new MissionPhotoResponseDTO(
                photo.getId(), photoViewUrl, photo.getCaption(),
                photo.getLatitude(), photo.getLongitude(), photo.getTakenAt()
        );
    }

    public static com.quespot.domain.mission.dto.res.MissionCourseListItemResponseDTO toCourseListItem(
            com.quespot.domain.mission.dto.MissionCourseListItemResultDTO result
    ) {
        com.quespot.domain.mission.entity.MissionCourse course = result.course();
        return new com.quespot.domain.mission.dto.res.MissionCourseListItemResponseDTO(
                course.getId(), course.getName(), course.getCoverImageUrl(), course.getRegionCode(),
                course.getTotalRewardPoint(), course.getBonusPoint(), course.getMissionCount(),
                course.getEstimatedMinutes(), result.myStatus()
        );
    }

    public static com.quespot.domain.mission.dto.res.MissionCourseDetailResponseDTO toCourseDetail(
            com.quespot.domain.mission.dto.MissionCourseDetailResultDTO result
    ) {
        com.quespot.domain.mission.entity.MissionCourse course = result.course();
        return new com.quespot.domain.mission.dto.res.MissionCourseDetailResponseDTO(
                course.getId(), course.getName(), course.getDescription(), course.getCoverImageUrl(),
                course.getRegionCode(), course.getTotalRewardPoint(), course.getBonusPoint(),
                course.getEstimatedMinutes(),
                result.missions().stream().map(MissionConverter::toCourseMissionItem).toList(),
                result.myStatus()
        );
    }

    private static com.quespot.domain.mission.dto.res.CourseMissionItemResponseDTO toCourseMissionItem(
            com.quespot.domain.mission.dto.CourseMissionItemResultDTO item
    ) {
        var courseMission = item.courseMission();
        var mission = courseMission.getMission();
        return new com.quespot.domain.mission.dto.res.CourseMissionItemResponseDTO(
                mission.getId(), courseMission.getSeq(), mission.getTitle(), mission.getSnapshotImageUrl(),
                mission.getRewardPoint(), item.status()
        );
    }

    public static com.quespot.domain.mission.dto.res.UnlockConditionResponseDTO toUnlockConditionResponse(boolean locked) {
        return new com.quespot.domain.mission.dto.res.UnlockConditionResponseDTO(
                locked, locked ? "코스 진행 중이며 앞 미션을 완료해야 합니다." : null
        );
    }

    public static com.quespot.domain.mission.dto.res.CourseAttemptResponseDTO toCourseAttemptResponse(
            com.quespot.domain.mission.entity.CourseAttempt attempt
    ) {
        return new com.quespot.domain.mission.dto.res.CourseAttemptResponseDTO(
                attempt.getId(), attempt.getCourse().getId(), attempt.getCourse().getName(),
                attempt.getStatus(), attempt.getStartedAt(), attempt.getCompletedAt(), attempt.getEarnedBonusPoint()
        );
    }

    public static com.quespot.domain.mission.dto.res.CourseAttemptListResponseDTO toCourseAttemptListResponse(
            java.util.List<com.quespot.domain.mission.entity.CourseAttempt> attempts
    ) {
        return new com.quespot.domain.mission.dto.res.CourseAttemptListResponseDTO(
                attempts.stream().map(MissionConverter::toCourseAttemptResponse).toList()
        );
    }

    public static MissionArchiveItemResponseDTO toArchiveItem(ArchivePhoto photo, String photoViewUrl) {
        return new MissionArchiveItemResponseDTO(
                photo.getId(), ArchivePhotoSource.ARCHIVE, photoViewUrl, photo.getCaption(),
                null, null, null,
                null, photo.getCreatedAt()
        );
    }

    public static MissionArchiveItemResponseDTO toArchiveItem(ArchiveFeedRowProjection row, String photoViewUrl) {
        MissionCategory category = row.getMissionCategory() == null
                ? null : MissionCategory.valueOf(row.getMissionCategory());
        return new MissionArchiveItemResponseDTO(
                row.getId(), ArchivePhotoSource.valueOf(row.getSource()), photoViewUrl, row.getCaption(),
                row.getMissionId(), row.getMissionTitle(), category,
                row.getCompletedAt(), row.getCreatedAt()
        );
    }

    public static MissionArchiveListResponseDTO toArchiveListResponse(
            List<MissionArchiveItemResponseDTO> items, String nextCursor, boolean hasNext
    ) {
        return new MissionArchiveListResponseDTO(
                items, nextCursor, hasNext
        );
    }

    public static CompletedMissionArchiveItemResponseDTO toCompletedMissionArchiveItem(
            CompletedMissionArchiveProjection row
    ) {
        return new CompletedMissionArchiveItemResponseDTO(
                row.getMissionId(), row.getSpotName(), row.getLatitude(), row.getLongitude(),
                row.getCompletedAt(), row.getEarnedPoint()
        );
    }

    public static MissionArchiveMapResponseDTO toArchiveMapResponse(
            List<CompletedMissionArchiveItemResponseDTO> completedMissions
    ) {
        long totalEarnedPoint = completedMissions.stream()
                .map(CompletedMissionArchiveItemResponseDTO::earnedPoint)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();

        return new MissionArchiveMapResponseDTO(
                new MissionArchiveFootprintResponseDTO(completedMissions.size(), totalEarnedPoint),
                completedMissions
        );
    }

    private static String preferredImage(Spot spot) {
        return hasText(spot.getImageUrl()) ? spot.getImageUrl() : spot.getThumbnailUrl();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
