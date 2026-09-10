package com.quespot.domain.reward.config;

import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.Stamp;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.StampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 앱 기동 시 배지·스탬프 마스터 데이터를 시드한다.
// 배지: 코드 기준으로 없으면 추가, 있으면 정의(condition_json 등)가 다를 때 갱신,
//       목록에서 빠진 활성 배지는 비활성화(#50). 참고 SQL: docs/seed_badges_stamps.sql
// 스탬프: 없는 것만 추가(8개, 서울만 활성).
@Component
@RequiredArgsConstructor
public class RewardMasterDataSeeder implements CommandLineRunner {

    private final BadgeRepository badgeRepository;
    private final StampRepository stampRepository;

    private record BadgeDefinition(String code, String name, String description, String conditionJson, int sortOrder) {
    }

    // 확정 배지 5개(#50). 부산 탐험은 조건 미정으로 보류, 웨스트 왕은 폐기.
    private static final List<BadgeDefinition> BADGES = List.of(
            new BadgeDefinition("FIRST_MISSION", "첫 미션", "첫 미션을 완료했어요",
                    "{\"metric\":\"MISSION_COMPLETED\",\"threshold\":1}", 1),
            new BadgeDefinition("EXPLORER", "탐험가", "서울의 서로 다른 구에서 미션 5개를 완료했어요",
                    "{\"metric\":\"MISSION_COMPLETED_DISTINCT_DISTRICT\",\"scope\":{\"regionCode\":\"11\"},\"threshold\":5}", 2),
            new BadgeDefinition("PHOTOGRAPHER", "사진작가", "아카이브에 사진 10장을 등록했어요",
                    "{\"metric\":\"PHOTO_REGISTERED\",\"threshold\":10}", 3),
            new BadgeDefinition("SEOUL_MASTER", "서울 마스터", "서울 미션 10개를 완료했어요",
                    "{\"metric\":\"MISSION_COMPLETED\",\"scope\":{\"regionCode\":\"11\"},\"threshold\":10}", 4),
            new BadgeDefinition("QUEST_KING", "퀘스트 왕", "코스 5개를 완주했어요",
                    "{\"metric\":\"COURSE_COMPLETED\",\"threshold\":5}", 5)
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedBadges();
        seedStamps();
    }

    private void seedBadges() {
        List<Badge> missing = new ArrayList<>();
        for (BadgeDefinition def : BADGES) {
            Optional<Badge> existing = badgeRepository.findByCode(def.code());
            if (existing.isEmpty()) {
                missing.add(Badge.seed(def.code(), def.name(), def.description(), def.conditionJson(), def.sortOrder()));
            } else if (needsUpdate(existing.get(), def)) {
                existing.get().updateMaster(def.name(), def.description(), def.conditionJson(), def.sortOrder());
            }
        }
        if (!missing.isEmpty()) {
            badgeRepository.saveAll(missing);
        }
        Set<String> knownCodes = BADGES.stream().map(BadgeDefinition::code).collect(Collectors.toSet());
        badgeRepository.findByIsActiveTrue().stream()
                .filter(badge -> !knownCodes.contains(badge.getCode()))
                .forEach(Badge::deactivate);
    }

    private boolean needsUpdate(Badge badge, BadgeDefinition def) {
        return !def.conditionJson().equals(badge.getConditionJson())
                || !def.name().equals(badge.getName())
                || !def.description().equals(badge.getDescription())
                || def.sortOrder() != badge.getSortOrder()
                || !Boolean.TRUE.equals(badge.getIsActive());
    }

    private void seedStamps() {
        List<Stamp> missing = Stream.of(
                        Stamp.seed("SEOUL", "서울", "11", 1, true),
                        Stamp.seed("BUSAN", "부산", "26", 2, false),
                        Stamp.seed("JEJU", "제주", "50", 3, false),
                        Stamp.seed("GYEONGJU", "경주", "47", 4, false),
                        Stamp.seed("YEOSU", "여수", "46", 5, false),
                        Stamp.seed("GANGNEUNG", "강릉", "42", 6, false),
                        Stamp.seed("JEONJU", "전주", "45", 7, false),
                        Stamp.seed("INCHEON", "인천", "28", 8, false)
                )
                .filter(stamp -> !stampRepository.existsByCode(stamp.getCode()))
                .toList();
        if (!missing.isEmpty()) {
            stampRepository.saveAll(missing);
        }
    }
}
