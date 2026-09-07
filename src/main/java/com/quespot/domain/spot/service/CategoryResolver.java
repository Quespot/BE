package com.quespot.domain.spot.service;

import com.quespot.domain.spot.entity.CategoryMapping;
import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.repository.CategoryMappingRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

// 246개 분류에 대해 매핑은 23행뿐이고 거의 바뀌지 않는다. 매 건마다 DB를 조회하지
// 않도록 기동 시 전체를 한 번 읽어 메모리에 올려둔다(재기동 전까지 고정 스냅샷).
@Component
public class CategoryResolver {

    private final Map<String, AppCategory> mappingsByLclsCode;

    public CategoryResolver(CategoryMappingRepository categoryMappingRepository) {
        this.mappingsByLclsCode = categoryMappingRepository.findAll().stream()
                .collect(Collectors.toMap(
                        CategoryMapping::getLclsCode,
                        CategoryMapping::getAppCategory,
                        // content_type_id별 오버라이드로 같은 lcls_code가 두 번 나올 수 있는데
                        // 지금은 content_type_id를 매칭에 안 써서 먼저 읽힌 값을 그대로 쓴다.
                        // 그런 오버라이드가 실제로 필요해지면 이 부분부터 손봐야 한다.
                        (existing, duplicate) -> existing
                ));
    }

    // lcls_systm3 -> lcls_systm2 -> lcls_systm1 순으로 조회해 먼저 매칭되는 것을
    // 쓴다. priority 컬럼(10/20/30)은 문서화용이고, 실제 우선순위는 이 조회 순서
    // 자체가 보장한다 — 각 레벨의 코드 길이가 이미 소분류>중분류>대분류 순이라
    // priority 값을 따로 비교할 필요가 없다.
    public AppCategory resolve(String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        AppCategory bySystm3 = lookup(lclsSystm3);
        if (bySystm3 != null) {
            return bySystm3;
        }

        AppCategory bySystm2 = lookup(lclsSystm2);
        if (bySystm2 != null) {
            return bySystm2;
        }

        AppCategory bySystm1 = lookup(lclsSystm1);
        if (bySystm1 != null) {
            return bySystm1;
        }

        return AppCategory.UNMAPPED;
    }

    private AppCategory lookup(String lclsCode) {
        // null 가드가 없으면 Collectors.toMap 결과(불변 Map)는 get(null)에서
        // NullPointerException을 던진다.
        if (lclsCode == null || lclsCode.isBlank()) {
            return null;
        }
        return mappingsByLclsCode.get(lclsCode);
    }
}
