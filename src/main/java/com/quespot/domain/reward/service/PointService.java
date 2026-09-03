package com.quespot.domain.reward.service;

import com.quespot.domain.reward.converter.RewardConverter;
import com.quespot.domain.reward.dto.res.PointResponseDTO;
import com.quespot.domain.reward.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;

    @Transactional(readOnly = true)
    public PointResponseDTO getPoints(Long userId) {
        return userPointRepository.findById(userId)
                .map(RewardConverter::toPointResponseDTO)
                .orElseGet(RewardConverter::emptyPointResponseDTO);
    }
}
