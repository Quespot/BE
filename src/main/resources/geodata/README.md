# 행정구역 경계 데이터

`seoul-municipalities.geojson`은 자치구 좌표 판정을 위해 사용하는 서울 시군구 경계입니다.

- 원본: [southkorea/seoul-maps](https://github.com/southkorea/seoul-maps/blob/master/juso/2015/json/seoul_municipalities_geo_simple.json)
- 원천: 서울시 행정구역 시군구 정보(JUSO, 2015)
- 라이선스: Apache License 2.0
- 좌표계: WGS84 경도/위도

다른 지역을 지원할 때는 동일한 `SIG_CD`, `SIG_KOR_NM` 속성을 가진 경계 데이터를 추가하고
`AdministrativeDistrictResolver`의 로딩 목록을 확장합니다.
