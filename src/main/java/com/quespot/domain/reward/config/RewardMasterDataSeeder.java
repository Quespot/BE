package com.quespot.domain.reward.config;

import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.Stamp;
import com.quespot.domain.reward.repository.BadgeRepository;
import com.quespot.domain.reward.repository.StampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

// 앱 기동 시 배지·스탬프 마스터 데이터를 시드한다. 로우별로 존재 여부를 확인해 없는 것만 추가한다.
@Component
@RequiredArgsConstructor
public class RewardMasterDataSeeder implements CommandLineRunner {

    private final BadgeRepository badgeRepository;
    private final StampRepository stampRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedBadges();
        seedStamps();
    }

    private void seedBadges() {
        List<Badge> missing = Stream.of(
                        Badge.seed("FIRST_MISSION", "첫 미션", "첫 미션을 완료했어요",
                                "{\"metric\":\"MISSION_COMPLETE_COUNT\",\"scope\":\"ALL\",\"threshold\":1}", 1),
                        Badge.seed("EXPLORER", "탐험가", "여러 스팟을 방문했어요",
                                "{\"metric\":\"SPOT_VISITED_COUNT\",\"scope\":\"ALL\",\"threshold\":10}", 2),
                        Badge.seed("PHOTOGRAPHER", "사진작가", "사진 인증을 여러 번 완료했어요",
                                "{\"metric\":\"PHOTO_VERIFICATION_COUNT\",\"scope\":\"ALL\",\"threshold\":10}", 3),
                        Badge.seed("SEOUL_MASTER", "서울 마스터", "서울 지역 미션을 다수 완료했어요",
                                "{\"metric\":\"MISSION_COMPLETE_COUNT\",\"scope\":\"REGION:SEOUL\",\"threshold\":20}", 4),
                        Badge.seed("BUSAN_EXPLORER", "부산 탐험", "부산 지역 미션을 다수 완료했어요",
                                "{\"metric\":\"MISSION_COMPLETE_COUNT\",\"scope\":\"REGION:BUSAN\",\"threshold\":10}", 5),
                        Badge.seed("WEST_KING", "웨스트 왕", "서쪽 지역 미션을 다수 완료했어요",
                                "{\"metric\":\"MISSION_COMPLETE_COUNT\",\"scope\":\"REGION:WEST\",\"threshold\":10}", 6)
                )
                .filter(badge -> !badgeRepository.existsByCode(badge.getCode()))
                .toList();

        if (!missing.isEmpty()) {
            badgeRepository.saveAll(missing);
        }
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
