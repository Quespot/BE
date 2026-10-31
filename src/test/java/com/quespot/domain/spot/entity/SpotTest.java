package com.quespot.domain.spot.entity;

import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SpotTest {

    private Spot buildSpot(String sourceContentId, String name, boolean showFlag, LocalDateTime sourceModifiedAt) {
        return Spot.builder()
                .source(SpotSource.TOUR_API)
                .sourceContentId(sourceContentId)
                .name(name)
                .latitude(new BigDecimal("37.5"))
                .longitude(new BigDecimal("127.0"))
                .appCategory(AppCategory.CULTURE)
                .categoryMappingVersion(1)
                .showFlag(showFlag)
                .sourceModifiedAt(sourceModifiedAt)
                .build();
    }

    @Test
    void builderSetsShowFlagFromParameterInsteadOfAlwaysTrue() {
        Spot spot = buildSpot("1", "경복궁", false, LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThat(spot.getShowFlag()).isFalse();
    }

    @Test
    void refreshCopiesContentFieldsButKeepsIdentity() {
        Spot existing = buildSpot("1", "옛이름", true, LocalDateTime.of(2026, 1, 1, 0, 0));
        // sourceContentId를 일부러 다르게 줘서, refresh()가 실수로 정체성 필드까지
        // 복사하면 이 테스트가 실제로 잡아내게 한다.
        Spot freshData = buildSpot("2", "새이름", false, LocalDateTime.of(2026, 2, 1, 0, 0));

        existing.refresh(freshData);

        assertThat(existing.getName()).isEqualTo("새이름");
        assertThat(existing.getShowFlag()).isFalse();
        assertThat(existing.getSourceModifiedAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
        assertThat(existing.getSourceContentId()).isEqualTo("1");
        assertThat(existing.getSource()).isEqualTo(SpotSource.TOUR_API);
    }
}
