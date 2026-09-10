package com.quespot.domain.like.service;

import com.quespot.domain.like.exception.LikeException;
import com.quespot.domain.like.exception.code.LikeErrorCode;
import com.quespot.domain.like.repository.SavedSpotRepository;
import com.quespot.domain.spot.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 좋아요와 별개 기능(화면이 다르다). 등록은 upsert로 멱등, 해제는 검증 없이 항상 성공.
@Service
@RequiredArgsConstructor
public class SavedSpotService {

    private final SavedSpotRepository savedSpotRepository;
    private final SpotRepository spotRepository;

    @Transactional
    public void save(Long userId, Long spotId) {
        spotRepository.findById(spotId)
                .filter(spot -> Boolean.TRUE.equals(spot.getShowFlag()))
                .orElseThrow(() -> new LikeException(LikeErrorCode.SPOT_NOT_FOUND));
        savedSpotRepository.upsert(userId, spotId);
    }

    @Transactional
    public void unsave(Long userId, Long spotId) {
        savedSpotRepository.deleteByUserIdAndSpot_Id(userId, spotId);
    }
}
