package com.quespot.domain.spot.entity;

import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SpotTest {

    private Spot buildSpot(String name, boolean showFlag, LocalDateTime sourceModifiedAt) {
        return Spot.builder()
                .source(SpotSource.TOUR_API)
                .sourceContentId("1")
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
        Spot spot = buildSpot("경복궁", false, LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThat(spot.getShowFlag()).isFalse();
    }

    @Test
    void refreshCopiesContentFieldsButKeepsIdentity() {
        Spot existing = buildSpot("옛이름", true, LocalDateTime.of(2026, 1, 1, 0, 0));
        Spot freshData = buildSpot("새이름", false, LocalDateTime.of(2026, 2, 1, 0, 0));

        existing.refresh(freshData);

        assertThat(existing.getName()).isEqualTo("새이름");
        assertThat(existing.getShowFlag()).isFalse();
        assertThat(existing.getSourceModifiedAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
        // 정체성 필드는 freshData와 우연히 같더라도 로직상 건드리지 않는다는 것을 보장하려면
        // sourceContentId를 바꿔서 전달했을 때도 existing이 원래 값을 유지해야 한다.
        assertThat(existing.getSource()).isEqualTo(SpotSource.TOUR_API);
    }
}
