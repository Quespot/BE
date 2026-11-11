# 행정구역 경계 데이터

좌표로 시군구를 판정하기 위해 사용하는 수도권 시군구 경계입니다. 속성은 모두 `SIG_CD`(행정표준 시군구 코드), `SIG_KOR_NM`을 사용합니다.

| 파일 | 지역 | 구역 수 | 원본 |
| --- | --- | --- | --- |
| `seoul-municipalities.geojson` | 서울특별시 | 25개 구 | [southkorea/seoul-maps](https://github.com/southkorea/seoul-maps/blob/master/juso/2015/json/seoul_municipalities_geo_simple.json) (JUSO 2015, Apache License 2.0) |
| `incheon-municipalities.geojson` | 인천광역시 | 11개 구·군 | [vuski/admdongkor](https://github.com/vuski/admdongkor) `ver20261001` (CC BY 4.0) |
| `gyeonggi-municipalities.geojson` | 경기도 | 31개 시·군 | [vuski/admdongkor](https://github.com/vuski/admdongkor) `ver20261001` (CC BY 4.0) |

- 좌표계: WGS84 경도/위도
- 인천·경기는 행정동 경계를 시군구별로 묶은 MultiPolygon입니다(행정동 경계를 합치지 않고 그대로 포함). 경기도 일반구는 소속 시로 묶었습니다.
- 인천은 2026년 7월 행정체제 개편(제물포구·영종구·검단구 신설, 서구 → 서해구)이 반영된 경계입니다.
- 좌표는 소수점 6자리로 반올림하고 약 3m 허용 오차로 단순화했습니다.

다른 지역을 지원할 때는 같은 속성을 가진 경계 데이터를 추가하고 `ServiceRegion`에 지역을 추가합니다.
