package com.quespot.domain.mission.enums;

import io.swagger.v3.oas.annotations.media.Schema;
// 아카이브 통합 피드(#45 확장)의 각 항목이 어디서 왔는지 구분한다.
// mission_photos.id와 archive_photos.id는 서로 다른 테이블의 auto-increment라
// 값이 겹칠 수 있다 — photoId만으로는 프론트가 항목을 유일하게 식별할 수
// 없어서 source를 같이 내려준다.
@Schema(description = "아카이브 사진 출처. MISSION = 미션 완료 후 POST /api/mission-attempts/{id}/photos로 등록, ARCHIVE = 미션과 무관하게 POST /api/users/me/archives로 자유 업로드")
public enum ArchivePhotoSource {
    MISSION,
    ARCHIVE
}
