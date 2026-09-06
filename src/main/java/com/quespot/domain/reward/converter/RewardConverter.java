package com.quespot.domain.reward.converter;

import com.quespot.domain.reward.dto.res.BadgeResponseDTO;
import com.quespot.domain.reward.dto.res.PointResponseDTO;
import com.quespot.domain.reward.dto.res.RewardActivityResponseDTO;
import com.quespot.domain.reward.dto.res.StampResponseDTO;
import com.quespot.domain.reward.entity.Badge;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.entity.Stamp;
import com.quespot.domain.reward.entity.UserBadge;
import com.quespot.domain.reward.entity.UserPoint;
import com.quespot.domain.reward.entity.UserStamp;

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

    public static RewardActivityResponseDTO toRewardActivityResponseDTO(RewardActivity activity) {
        Integer amount = activity.getPointTransaction() != null
                ? activity.getPointTransaction().getAmount()
                : null;

        return new RewardActivityResponseDTO(
                activity.getId(),
                activity.getActivityType(),
                activity.getTitle(),
                amount,
                activity.getCreatedAt()
        );
    }

    public static BadgeResponseDTO toBadgeResponseDTO(Badge badge, UserBadge acquired) {
        return new BadgeResponseDTO(
                badge.getId(),
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl(),
                acquired != null,
                acquired != null ? acquired.getAcquiredAt() : null
        );
    }

    public static StampResponseDTO toStampResponseDTO(Stamp stamp, UserStamp acquired) {
        return new StampResponseDTO(
                stamp.getId(),
                stamp.getCode(),
                stamp.getName(),
                stamp.getRegionCode(),
                stamp.getIconUrl(),
                stamp.getIsActive(),
                acquired != null,
                acquired != null ? acquired.getAcquiredAt() : null
        );
    }
}
