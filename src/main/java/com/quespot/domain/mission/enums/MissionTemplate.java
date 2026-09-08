package com.quespot.domain.mission.enums;

import com.quespot.domain.spot.enums.AppCategory;

import java.util.Optional;

public enum MissionTemplate {
    HISTORY_LOCATION(
            MissionCategory.HISTORY,
            "%s에서 역사 흔적 찾기",
            "%s에 방문해 위치 인증을 완료해 주세요. 기억하고 싶은 순간은 사진으로 자유롭게 남길 수 있어요.",
            100,
            30
    ),
    CULTURE_LOCATION(
            MissionCategory.CULTURE,
            "%s 문화 체험하기",
            "%s에 방문해 위치 인증을 완료해 주세요. 기억하고 싶은 순간은 사진으로 자유롭게 남길 수 있어요.",
            100,
            30
    ),
    NATURE_LOCATION(
            MissionCategory.NATURE,
            "%s 자연 풍경 감상하기",
            "%s에 도착해 위치 인증을 완료해 주세요. 기억하고 싶은 순간은 사진으로 자유롭게 남길 수 있어요.",
            120,
            40
    ),
    FOOD_LOCATION(
            MissionCategory.FOOD,
            "%s 맛집 방문하기",
            "%s에 방문해 위치 인증을 완료해 주세요. 기억하고 싶은 순간은 사진으로 자유롭게 남길 수 있어요.",
            80,
            20
    ),
    NIGHT_VIEW_LOCATION(
            MissionCategory.NIGHT_VIEW,
            "%s 야경 감상하기",
            "%s에 도착해 위치 인증을 완료해 주세요. 기억하고 싶은 순간은 사진으로 자유롭게 남길 수 있어요.",
            120,
            30
    ),
    ETC_LOCATION(
            MissionCategory.ETC,
            "%s 방문하기",
            "%s에 방문해 위치 인증을 완료해 주세요. 기억하고 싶은 순간은 사진으로 자유롭게 남길 수 있어요.",
            80,
            20
    );

    private final MissionCategory category;
    private final String titleFormat;
    private final String descriptionFormat;
    private final int rewardPoint;
    private final int estimatedMinutes;

    MissionTemplate(
            MissionCategory category,
            String titleFormat,
            String descriptionFormat,
            int rewardPoint,
            int estimatedMinutes
    ) {
        this.category = category;
        this.titleFormat = titleFormat;
        this.descriptionFormat = descriptionFormat;
        this.rewardPoint = rewardPoint;
        this.estimatedMinutes = estimatedMinutes;
    }

    public static Optional<MissionTemplate> fromSpotCategory(AppCategory category) {
        return switch (category) {
            case HISTORY -> Optional.of(HISTORY_LOCATION);
            case CULTURE -> Optional.of(CULTURE_LOCATION);
            case NATURE -> Optional.of(NATURE_LOCATION);
            case FOOD -> Optional.of(FOOD_LOCATION);
            case NIGHT_VIEW -> Optional.of(NIGHT_VIEW_LOCATION);
            case UNMAPPED, EXCLUDED -> Optional.of(ETC_LOCATION);
        };
    }

    public String title(String spotName) {
        return titleFormat.formatted(spotName);
    }

    public String description(String spotName) {
        return descriptionFormat.formatted(spotName);
    }

    public MissionCategory getCategory() {
        return category;
    }

    public int getRewardPoint() {
        return rewardPoint;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }
}
