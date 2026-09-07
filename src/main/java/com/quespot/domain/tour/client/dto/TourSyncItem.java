package com.quespot.domain.tour.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 필드 케이스가 불규칙하다(대부분 소문자, 일부만 camelCase)라 NamingStrategy로
// 못 잡는다. 값이 없으면 null이 아니라 ""로 온다 — 파싱은 정제 단계(다음 이슈)에서.
// cat1/cat2/cat3, areacode, sigungucode는 포털이 "삭제예정"으로 명시한 죽은
// 필드라 선언하지 않는다 — 응답엔 여전히 딸려 오므로 @JsonIgnoreProperties로
// 미선언 필드를 허용해야 파싱이 깨지지 않는다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourSyncItem(
        @JsonProperty("contentid") String contentid,
        @JsonProperty("contenttypeid") String contenttypeid,
        @JsonProperty("title") String title,
        @JsonProperty("addr1") String addr1,
        @JsonProperty("addr2") String addr2,
        @JsonProperty("zipcode") String zipcode,
        @JsonProperty("tel") String tel,
        @JsonProperty("mapx") String mapx,
        @JsonProperty("mapy") String mapy,
        @JsonProperty("mlevel") String mlevel,
        @JsonProperty("firstimage") String firstimage,
        @JsonProperty("firstimage2") String firstimage2,
        @JsonProperty("createdtime") String createdtime,
        @JsonProperty("modifiedtime") String modifiedtime,
        @JsonProperty("showflag") String showflag,
        @JsonProperty("cpyrhtDivCd") String cpyrhtDivCd,
        @JsonProperty("lDongRegnCd") String lDongRegnCd,
        @JsonProperty("lDongSignguCd") String lDongSignguCd,
        @JsonProperty("lclsSystm1") String lclsSystm1,
        @JsonProperty("lclsSystm2") String lclsSystm2,
        @JsonProperty("lclsSystm3") String lclsSystm3
) {
}
