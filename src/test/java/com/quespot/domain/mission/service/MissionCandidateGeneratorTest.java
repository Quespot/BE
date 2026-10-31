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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionCandidateGeneratorTest {

    private SpotRepository spotRepository;
    private MissionCandidateRepository missionCandidateRepository;
    private MissionCandidateWriter missionCandidateWriter;
    private MissionCandidateGenerator generator;

    @BeforeEach
    void setUp() {
        spotRepository = mock(SpotRepository.class);
        missionCandidateRepository = mock(MissionCandidateRepository.class);
        missionCandidateWriter = mock(MissionCandidateWriter.class);
        generator = new MissionCandidateGenerator(
                spotRepository,
                missionCandidateRepository,
                missionCandidateWriter
        );
    }

    @Test
    void generatesCandidateForEligibleSpot() {
        Spot spot = spot(1L, "경복궁", AppCategory.HISTORY, "https://example.com/image.jpg");
        when(spotRepository.findMissionCandidateEligibleSpots(eq(SpotSource.TOUR_API), eq("11"), anyList()))
                .thenReturn(List.of(spot));
        when(missionCandidateRepository.findGenerationKeys(1)).thenReturn(List.of());

        MissionCandidateGenerationResponseDTO result = generator.generate();

        assertThat(result.eligibleSpotCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.skippedDuplicateCount()).isZero();
        verify(missionCandidateWriter).save(any(MissionCandidate.class));
    }

    @Test
    void generatesCandidatesOnlyForSelectedCategory() {
        Spot spot = spot(1L, "경복궁", AppCategory.HISTORY, "https://example.com/image.jpg");
        when(spotRepository.findMissionCandidateEligibleSpots(
                SpotSource.TOUR_API,
                "11",
                List.of(AppCategory.HISTORY)
        )).thenReturn(List.of(spot));
        when(missionCandidateRepository.findGenerationKeys(1)).thenReturn(List.of());

        MissionCandidateGenerationResponseDTO result = generator.generate(MissionCategory.HISTORY);

        assertThat(result.createdCount()).isEqualTo(1);
        verify(spotRepository).findMissionCandidateEligibleSpots(
                SpotSource.TOUR_API,
                "11",
                List.of(AppCategory.HISTORY)
        );
    }

    @Test
    void skipsCandidateAlreadyGeneratedBySameVersion() {
        Spot spot = spot(1L, "경복궁", AppCategory.HISTORY, "https://example.com/image.jpg");
        when(spotRepository.findMissionCandidateEligibleSpots(eq(SpotSource.TOUR_API), eq("11"), anyList()))
                .thenReturn(List.of(spot));
        when(missionCandidateRepository.findGenerationKeys(1)).thenReturn(List.of(
                new MissionCandidateGenerationKeyDTO(
                        1L,
                        MissionTemplate.HISTORY_LOCATION
                )
        ));

        MissionCandidateGenerationResponseDTO result = generator.generate();

        assertThat(result.createdCount()).isZero();
        assertThat(result.skippedDuplicateCount()).isEqualTo(1);
    }

    @Test
    void generatesEtcCandidatesForUnmappedAndExcludedSpots() {
        Spot unmappedSpot = spot(1L, "기타 관광지", AppCategory.UNMAPPED, "https://example.com/1.jpg");
        Spot excludedSpot = spot(2L, "기타 장소", AppCategory.EXCLUDED, "https://example.com/2.jpg");
        when(spotRepository.findMissionCandidateEligibleSpots(eq(SpotSource.TOUR_API), eq("11"), anyList()))
                .thenReturn(List.of(unmappedSpot, excludedSpot));
        when(missionCandidateRepository.findGenerationKeys(1)).thenReturn(List.of());

        MissionCandidateGenerationResponseDTO result = generator.generate(MissionCategory.ETC);

        assertThat(result.createdCount()).isEqualTo(2);
        verify(missionCandidateWriter, times(2)).save(argThat(candidate ->
                candidate.getSuggestedCategory() == MissionCategory.ETC
                        && candidate.getTemplateCode() == MissionTemplate.ETC_LOCATION
        ));
        verify(spotRepository).findMissionCandidateEligibleSpots(
                SpotSource.TOUR_API,
                "11",
                List.of(AppCategory.UNMAPPED, AppCategory.EXCLUDED)
        );
    }

    @Test
    void treatsConcurrentDuplicateInsertAsSkippedDuplicate() {
        Spot spot = spot(1L, "경복궁", AppCategory.HISTORY, "https://example.com/image.jpg");
        when(spotRepository.findMissionCandidateEligibleSpots(eq(SpotSource.TOUR_API), eq("11"), anyList()))
                .thenReturn(List.of(spot));
        when(missionCandidateRepository.findGenerationKeys(1)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(missionCandidateWriter).save(any(MissionCandidate.class));

        MissionCandidateGenerationResponseDTO result = generator.generate();

        assertThat(result.createdCount()).isZero();
        assertThat(result.skippedDuplicateCount()).isEqualTo(1);
    }

    private Spot spot(Long id, String name, AppCategory category, String imageUrl) {
        Spot spot = mock(Spot.class);
        when(spot.getId()).thenReturn(id);
        when(spot.getName()).thenReturn(name);
        when(spot.getAppCategory()).thenReturn(category);
        when(spot.getImageUrl()).thenReturn(imageUrl);
        return spot;
    }
}
