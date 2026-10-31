package com.quespot.domain.spot;

import com.quespot.domain.spot.service.SpotRefiner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// tour_contents_raw -> spots 정제 배치의 "수동 실행 전용" 진입점이다. 외부 API는
// 안 쓰지만 로컬 DB를 실제로 바꾸므로 CI·./gradlew test에서 자동으로 돌면 안 된다.
// 실행하려면:
//   1) 아래 @Disabled를 잠깐 지우거나 주석 처리한다
//   2) 이 테스트 하나만 돌린다:
//      ./gradlew test --tests "com.quespot.domain.spot.SpotRefinementManualTest" --info
//   3) 결과 확인 후 반드시 @Disabled를 원상복구하고 커밋한다
// 몇 번이든 재실행 가능하다 — 이미 정제된 건은 source_modified_at이 같으면
// 스킵되고, spots를 통째로 지우고 다시 돌려도 정상 동작한다(멱등).
@SpringBootTest
@Disabled("tour_contents_raw -> spots 정제 배치 — 로컬 DB를 실제로 바꾸므로 수동 실행 전용")
class SpotRefinementManualTest {

    @Autowired
    private SpotRefiner spotRefiner;

    @Test
    void refine() {
        spotRefiner.refine();
    }
}
