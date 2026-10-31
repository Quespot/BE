package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.MissionCandidateGenerationKeyDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateGenerationResponseDTO;
import com.quespot.domain.mission.entity.MissionCandidate;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.mission.repository.MissionCandidateRepository;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.domain.spot.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionCandidateGenerator {

    public static final int GENERATOR_VERSION = 1;

    private static final String SEOUL_REGION_CODE = "11";
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
                SEOUL_REGION_CODE,
                spotCategories
        ).stream().filter(this::isUsable).toList();
        Set<GenerationKey> existingKeys = missionCandidateRepository
                .findGenerationKeys(GENERATOR_VERSION)
                .stream()
                .map(GenerationKey::from)
                .collect(Collectors.toSet());

        int createdCount = 0;
        int skippedDuplicateCount = 0;

        for (Spot spot : spots) {
            MissionTemplate template = MissionTemplate.fromSpotCategory(spot.getAppCategory()).orElse(null);
            if (template == null) {
                continue;
            }

            if (existingKeys.contains(new GenerationKey(spot.getId(), template))) {
                skippedDuplicateCount++;
                continue;
            }

            try {
                missionCandidateWriter.save(MissionCandidate.generate(spot, template, GENERATOR_VERSION));
                createdCount++;
            } catch (DataIntegrityViolationException exception) {
                skippedDuplicateCount++;
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

    private record GenerationKey(Long spotId, MissionTemplate templateCode) {

        private static GenerationKey from(MissionCandidateGenerationKeyDTO key) {
            return new GenerationKey(key.spotId(), key.templateCode());
        }
    }
}
