package com.quespot.domain.spot.service;

import com.quespot.domain.spot.entity.CategoryMapping;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.repository.CategoryMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryResolverTest {

    private CategoryResolver categoryResolver;

    @BeforeEach
    void setUp() {
        CategoryMappingRepository categoryMappingRepository = mock(CategoryMappingRepository.class);
        when(categoryMappingRepository.findAll()).thenReturn(realSeedRows());

        categoryResolver = new CategoryResolver(categoryMappingRepository);
    }

    // docs/seed_category_mappings.sql과 동일한 23행. 실제 시드가 바뀌면 이 테스트도
    // 같이 맞춰야 한다.
    private List<CategoryMapping> realSeedRows() {
        return List.of(
                // 대분류 (priority 10)
                CategoryMapping.seed("HS", null, AppCategory.HISTORY, 10, 1),
                CategoryMapping.seed("NA", null, AppCategory.NATURE, 10, 1),
                CategoryMapping.seed("FD", null, AppCategory.FOOD, 10, 1),
                CategoryMapping.seed("VE", null, AppCategory.CULTURE, 10, 1),
                CategoryMapping.seed("AC", null, AppCategory.EXCLUDED, 10, 1),
                CategoryMapping.seed("EV", null, AppCategory.EXCLUDED, 10, 1),
                CategoryMapping.seed("C01", null, AppCategory.EXCLUDED, 10, 1),
                CategoryMapping.seed("LS", null, AppCategory.EXCLUDED, 10, 1),
                // 중분류 (priority 20)
                CategoryMapping.seed("VE03", null, AppCategory.NATURE, 20, 1),
                CategoryMapping.seed("VE05", null, AppCategory.EXCLUDED, 20, 1),
                CategoryMapping.seed("VE08", null, AppCategory.EXCLUDED, 20, 1),
                CategoryMapping.seed("VE10", null, AppCategory.EXCLUDED, 20, 1),
                CategoryMapping.seed("VE11", null, AppCategory.EXCLUDED, 20, 1),
                CategoryMapping.seed("SH06", null, AppCategory.CULTURE, 20, 1),
                // 소분류 (priority 30)
                CategoryMapping.seed("VE010200", null, AppCategory.NIGHT_VIEW, 30, 1),
                CategoryMapping.seed("VE040300", null, AppCategory.NATURE, 30, 1),
                CategoryMapping.seed("VE090500", null, AppCategory.EXCLUDED, 30, 1),
                CategoryMapping.seed("VE090600", null, AppCategory.EXCLUDED, 30, 1),
                CategoryMapping.seed("VE070400", null, AppCategory.EXCLUDED, 30, 1),
                CategoryMapping.seed("VE120200", null, AppCategory.EXCLUDED, 30, 1),
                CategoryMapping.seed("FD040300", null, AppCategory.EXCLUDED, 30, 1),
                CategoryMapping.seed("EX010100", null, AppCategory.CULTURE, 30, 1),
                CategoryMapping.seed("EX060100", null, AppCategory.CULTURE, 30, 1)
        );
    }

    @Test
    void fallsBackToDaebunryuWhenNoSubMappingExists() {
        // HS010100 -> 소분류/중분류 매핑 없음 -> 대분류 HS -> HISTORY
        AppCategory result = categoryResolver.resolve("HS", "HS01", "HS010100");

        assertThat(result).isEqualTo(AppCategory.HISTORY);
    }

    @Test
    void sobunryuBeatsDaebunryu_yongmaSkywalk() {
        // VE010200(용마산 스카이워크) 소분류가 대분류 CULTURE를 이기고 NIGHT_VIEW
        AppCategory result = categoryResolver.resolve("VE", "VE01", "VE010200");

        assertThat(result).isEqualTo(AppCategory.NIGHT_VIEW);
    }

    @Test
    void jungbunryuBeatsDaebunryu_gamsaGarden() {
        // 감사의 정원: 소분류 매핑 없음 -> 중분류 VE03이 대분류를 이기고 NATURE
        AppCategory result = categoryResolver.resolve("VE", "VE03", "VE030500");

        assertThat(result).isEqualTo(AppCategory.NATURE);
    }

    @Test
    void sobunryuMatch_jongnoDulegil() {
        // 종로둘레길: 소분류 VE040300 직접 매칭 -> NATURE
        AppCategory result = categoryResolver.resolve("VE", "VE04", "VE040300");

        assertThat(result).isEqualTo(AppCategory.NATURE);
    }

    @Test
    void fallsBackToDaebunryu_exhibitionHall() {
        // 전시관: 소분류/중분류(VE07) 매핑 없음 -> 대분류 VE -> CULTURE
        AppCategory result = categoryResolver.resolve("VE", "VE07", "VE070300");

        assertThat(result).isEqualTo(AppCategory.CULTURE);
    }

    @Test
    void fallsBackToDaebunryu_cafe() {
        // 카페: 소분류/중분류(FD05) 매핑 없음 -> 대분류 FD -> FOOD
        AppCategory result = categoryResolver.resolve("FD", "FD05", "FD050100");

        assertThat(result).isEqualTo(AppCategory.FOOD);
    }

    @Test
    void unmappedWhenNoRuleMatchesAtAnyLevel_kokoriColor() {
        // 코코리컬러: EX070200/EX07/EX 전부 매핑 없음 -> UNMAPPED
        AppCategory result = categoryResolver.resolve("EX", "EX07", "EX070200");

        assertThat(result).isEqualTo(AppCategory.UNMAPPED);
    }

    @Test
    void sobunryuOnlyOverride_hanbokNam() {
        // 한복남: EX는 대분류 매핑이 없지만 소분류 EX010100만 CULTURE로 살아있음
        AppCategory result = categoryResolver.resolve("EX", "EX01", "EX010100");

        assertThat(result).isEqualTo(AppCategory.CULTURE);
    }

    @Test
    void blankLclsSystm3SkipsToJungbunryu() {
        // lcls_systm3가 ""이면 그 단계를 건너뛰고 중분류(VE03)로 내려간다
        AppCategory result = categoryResolver.resolve("VE", "VE03", "");

        assertThat(result).isEqualTo(AppCategory.NATURE);
    }

    @Test
    void nullLclsSystm3AndSystm2SkipDownToDaebunryu() {
        // null도 빈 문자열과 동일하게 건너뛴다 (불변 Map.get(null) NPE 방지 겸)
        AppCategory result = categoryResolver.resolve("HS", null, null);

        assertThat(result).isEqualTo(AppCategory.HISTORY);
    }

    @Test
    void currentVersionIsMaxVersionAcrossLoadedMappings() {
        assertThat(categoryResolver.getCurrentVersion()).isEqualTo(1);
    }

    @Test
    void duplicateLclsCodeKeepsHighestVersionMapping() {
        // content_type_id별 오버라이드로 같은 lcls_code가 두 번 나올 수 있다(스키마의
        // UNIQUE (lcls_code, content_type_id, version)가 이걸 허용한다). 이때는
        // findAll()이 반환하는 순서와 무관하게 version이 더 높은 쪽이 이겨야 한다.
        CategoryMappingRepository categoryMappingRepository = mock(CategoryMappingRepository.class);
        when(categoryMappingRepository.findAll()).thenReturn(List.of(
                CategoryMapping.seed("HS", null, AppCategory.HISTORY, 10, 1),
                CategoryMapping.seed("HS", 12, AppCategory.CULTURE, 10, 2)
        ));

        CategoryResolver resolver = new CategoryResolver(categoryMappingRepository);

        assertThat(resolver.resolve("HS", null, null)).isEqualTo(AppCategory.CULTURE);
    }
}
