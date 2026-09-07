package com.quespot.domain.spot.config;

import com.quespot.domain.spot.entity.CategoryMapping;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.repository.CategoryMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

// 앱 기동 시 분류체계 -> 앱 카테고리 매핑 마스터 데이터를 시드한다. 로우별로 존재
// 여부를 확인해 없는 것만 추가한다. docs/seed_category_mappings.sql(로컬 전용
// 참고 파일)과 동일한 23행 — 그쪽이 바뀌면 여기도 같이 맞춰야 한다.
@Component
@RequiredArgsConstructor
public class CategoryMappingSeeder implements CommandLineRunner {

    private final CategoryMappingRepository categoryMappingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<CategoryMapping> missing = Stream.of(
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
                )
                .filter(mapping -> !categoryMappingRepository.existsByLclsCodeAndContentTypeIdAndVersion(
                        mapping.getLclsCode(), mapping.getContentTypeId(), mapping.getVersion()
                ))
                .toList();

        if (!missing.isEmpty()) {
            categoryMappingRepository.saveAll(missing);
        }
    }
}
