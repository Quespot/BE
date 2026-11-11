package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.MissionCandidateGenerationKeyDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateGenerationResponseDTO;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.ServiceRegion;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionCandidateGenerator {

    public static final int GENERATOR_VERSION = 1;
    private static final int CHUNK_SIZE = 200;

    private static final List<AppCategory> ELIGIBLE_CATEGORIES = List.copyOf(EnumSet.of(
            AppCategory.HISTORY,
            AppCategory.CULTURE,
            AppCategory.NATURE,
            AppCategory.FOOD,
            AppCategory.NIGHT_VIEW,
            AppCategory.UNMAPPED,
            AppCategory.EXCLUDED
    ));

    private final SpotRepository spotRepository;
    private final MissionCandidateRepository missionCandidateRepository;
    private final MissionCandidateWriter missionCandidateWriter;

    // 전체 미션 후보 생성 로직
    @Transactional
    public MissionCandidateGenerationResponseDTO generate() {
        return generateForSpotCategories(ELIGIBLE_CATEGORIES);
    }

    // 카테고리별 미션 후보 생성 로직
    @Transactional
    public MissionCandidateGenerationResponseDTO generate(MissionCategory category) {
        List<AppCategory> spotCategories = category == MissionCategory.ETC
                ? List.of(AppCategory.UNMAPPED, AppCategory.EXCLUDED)
                : List.of(AppCategory.valueOf(category.name()));
        return generateForSpotCategories(spotCategories);
    }

    private MissionCandidateGenerationResponseDTO generateForSpotCategories(List<AppCategory> spotCategories) {
        List<Spot> spots = spotRepository.findMissionCandidateEligibleSpots(
                SpotSource.TOUR_API,
                ServiceRegion.regionCodes(),
                spotCategories
        ).stream().filter(this::isUsable).toList();
        Set<ExistingKey> existingKeys = missionCandidateRepository
                .findGenerationKeys(GENERATOR_VERSION)
                .stream()
                .map(ExistingKey::from)
                .collect(Collectors.toSet());

        int skippedDuplicateCount = 0;
        List<GenerationKey> targets = new ArrayList<>();
        for (Spot spot : spots) {
            MissionTemplate template = MissionTemplate.fromSpotCategory(spot.getAppCategory()).orElse(null);
            if (template == null) {
                continue;
            }
            GenerationKey key = new GenerationKey(spot, template);
            if (existingKeys.contains(key.toExistingKey())) {
                skippedDuplicateCount++;
                continue;
            }
            targets.add(key);
        }

        int createdCount = 0;
        for (int i = 0; i < targets.size(); i += CHUNK_SIZE) {
            List<GenerationKey> chunk = targets.subList(i, Math.min(i + CHUNK_SIZE, targets.size()));
            try {
                missionCandidateWriter.saveAll(chunk.stream().map(GenerationKey::toCandidate).toList());
                createdCount += chunk.size();
            } catch (RuntimeException chunkFailure) {
                // 청크 안에 동시 생성된 중복 등이 있으면 청크 전체가 롤백된다.
                // 건별 트랜잭션으로 다시 저장해 실패한 건만 중복으로 센다.
                for (GenerationKey key : chunk) {
                    try {
                        missionCandidateWriter.save(key.toCandidate());
                        createdCount++;
                    } catch (DataIntegrityViolationException exception) {
                        skippedDuplicateCount++;
                    }
                }
            }
        }

        return new MissionCandidateGenerationResponseDTO(
                spots.size(),
                createdCount,
                skippedDuplicateCount
        );
    }

    private boolean isUsable(Spot spot) {
        return hasText(spot.getName())
                && (hasText(spot.getImageUrl()) || hasText(spot.getThumbnailUrl()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ExistingKey(Long spotId, MissionTemplate templateCode) {
        private static ExistingKey from(MissionCandidateGenerationKeyDTO key) {
            return new ExistingKey(key.spotId(), key.templateCode());
        }
    }

    private record GenerationKey(Spot spot, MissionTemplate template) {
        private ExistingKey toExistingKey() {
            return new ExistingKey(spot.getId(), template);
        }

        // 청크가 롤백되면 저장 시도한 엔티티에 ID가 남을 수 있어, 저장할 때마다 새로 만든다.
        private MissionCandidate toCandidate() {
            return MissionCandidate.generate(spot, template, GENERATOR_VERSION);
        }
    }
}
