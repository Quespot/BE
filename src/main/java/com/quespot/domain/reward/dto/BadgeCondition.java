package com.quespot.domain.reward.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quespot.domain.reward.enums.AchievementMetric;

import java.util.Optional;

// badges.condition_json 파싱 결과. 서비스 내부용이라 dto/res가 아니라 dto/ 바로 아래.
// 파싱 실패·미지 metric은 Optional.empty — 마스터 데이터 오류가 미션 완료를
// 실패시키면 안 되므로 호출부가 WARN 로그 후 건너뛴다.
public record BadgeCondition(AchievementMetric metric, String regionCode, int threshold) {

    public static Optional<BadgeCondition> parse(ObjectMapper objectMapper, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode metricNode = root.get("metric");
            JsonNode thresholdNode = root.get("threshold");
            if (metricNode == null || thresholdNode == null || !thresholdNode.canConvertToInt()) {
                return Optional.empty();
            }
            AchievementMetric metric = AchievementMetric.valueOf(metricNode.asText());
            // threshold <= 0이면 다음 이벤트에서 무조건 충족돼 배지가 잘못 지급된다 — 마스터 오류로 취급.
            int threshold = thresholdNode.asInt();
            if (threshold <= 0) {
                return Optional.empty();
            }
            JsonNode scope = root.get("scope");
            String regionCode = scope != null && scope.hasNonNull("regionCode") ? scope.get("regionCode").asText() : null;
            return Optional.of(new BadgeCondition(metric, regionCode, threshold));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
