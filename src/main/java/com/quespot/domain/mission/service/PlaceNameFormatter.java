package com.quespot.domain.mission.service;

import java.util.regex.Pattern;

// MissionCourseGenerationService가 코스 name/description을 자동 생성할 때
// 장소명을 다듬는 데 쓰는 순수 정적 유틸(#39 재설계). Spring 빈이 아니다.
public final class PlaceNameFormatter {

    private static final int MAX_LENGTH = 12;
    private static final Pattern TRAILING_PAREN = Pattern.compile("\\s*\\([^)]*\\)\\s*$");

    private PlaceNameFormatter() {
    }

    public static String clean(String rawName) {
        String stripped = TRAILING_PAREN.matcher(rawName).replaceAll("").strip();
        String base = stripped.isEmpty() ? rawName.strip() : stripped;
        return base.length() <= MAX_LENGTH ? base : base.substring(0, MAX_LENGTH) + "…";
    }
}
