package com.quespot.domain.reward.converter;

import com.quespot.domain.reward.dto.res.PointResponseDTO;
import com.quespot.domain.reward.entity.UserPoint;

public class RewardConverter {

    private RewardConverter() {
    }

    public static PointResponseDTO toPointResponseDTO(UserPoint userPoint) {
        return new PointResponseDTO(
                userPoint.getBalance(),
                userPoint.getTotalEarned(),
                userPoint.getTotalSpent()
        );
    }

    public static PointResponseDTO emptyPointResponseDTO() {
        return new PointResponseDTO(0, 0, 0);
    }
}
