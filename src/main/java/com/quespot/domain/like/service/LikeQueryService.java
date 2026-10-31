package com.quespot.domain.like.service;

import com.quespot.domain.like.converter.LikeConverter;
import com.quespot.domain.like.dto.LikedCourseRowDTO;
import com.quespot.domain.like.dto.res.LikedCourseListResponseDTO;
import com.quespot.domain.like.dto.res.LikedMissionListResponseDTO;
import com.quespot.domain.like.repository.LikeRepository;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 좋아요 목록은 본인 것만 대상이라 규모가 작아 페이징을 두지 않는다 — 커서가
// 필요해지면 그때 추가한다. 장소 탭은 없앴다(미션 = 장소, #50).
@Service
@RequiredArgsConstructor
public class LikeQueryService {

    private final LikeRepository likeRepository;
    private final CourseAttemptRepository courseAttemptRepository;

    @Transactional(readOnly = true)
    public LikedMissionListResponseDTO getLikedMissions(Long userId) {
        return LikeConverter.toLikedMissionList(likeRepository.findLikedMissions(userId));
    }

    @Transactional(readOnly = true)
    public LikedCourseListResponseDTO getLikedCourses(Long userId) {
        List<LikedCourseRowDTO> rows = likeRepository.findLikedCourses(userId);
        List<Long> courseIds = rows.stream().map(row -> row.course().getId()).toList();

        // 코스당 이 유저의 CourseAttempt는 최대 1건(비공개 코스, 재시작 경로 없음)이라
        // courseId → status 맵으로 한 번에 채운다(MissionCourseQueryService와 같은 방식).
        Map<Long, CourseAttemptStatus> statusByCourseId = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseAttemptRepository.findByUserIdAndCourseIdIn(userId, courseIds)
                    .forEach(ca -> statusByCourseId.put(ca.getCourse().getId(), ca.getStatus()));
        }
        return LikeConverter.toLikedCourseList(rows, statusByCourseId);
    }
}
