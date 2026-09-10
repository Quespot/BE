package com.quespot.domain.reward.service;

import com.quespot.domain.reward.converter.RewardConverter;
import com.quespot.domain.reward.dto.res.PointResponseDTO;
import com.quespot.domain.reward.entity.PointTransaction;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.entity.UserPoint;
import com.quespot.domain.reward.enums.ActivityType;
import com.quespot.domain.reward.exception.RewardException;
import com.quespot.domain.reward.exception.code.RewardErrorCode;
import com.quespot.domain.reward.repository.PointTransactionRepository;
import com.quespot.domain.reward.repository.RewardActivityRepository;
import com.quespot.domain.reward.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final RewardActivityRepository rewardActivityRepository;

    @Transactional(readOnly = true)
    public PointResponseDTO getPoints(Long userId) {
        return userPointRepository.findById(userId)
                .map(RewardConverter::toPointResponseDTO)
                .orElseGet(RewardConverter::emptyPointResponseDTO);
    }

    // REQUIRES_NEW를 쓰지 않는다. 호출부(MissionArrivalService.arrival)가
    // 이미 열어둔 트랜잭션에 그대로 합류해야 한다 — 미션 완료와 포인트
    // 지급은 하나의 사건이라 따로 커밋되면 안 된다. 동시 경합은
    // point_transactions의 UNIQUE(user_id, type, reference_type,
    // reference_id)가 막는다(애플리케이션 락 없음).
    @Transactional
    public void credit(
            Long userId,
            int amount,
            String type,
            String referenceType,
            Long referenceId,
            String activityTitle
    ) {
        userPointRepository.creditBalance(userId, amount);
        UserPoint userPoint = userPointRepository.findById(userId).orElseThrow();

        PointTransaction transaction = pointTransactionRepository.save(
                PointTransaction.earn(userId, amount, type, referenceType, referenceId, userPoint.getBalance())
        );
        rewardActivityRepository.save(
                RewardActivity.of(userId, ActivityType.POINT_EARNED, activityTitle, transaction, referenceType, referenceId)
        );
    }

    // 차감도 credit처럼 호출부(ItemPurchaseService) 트랜잭션에 합류한다 — 아이템
    // 지급과 포인트 차감은 하나의 사건이다. 같은 (type, reference)로 두 번 차감되는
    // 건 point_transactions UNIQUE가 막는다. 반환값은 차감 후 잔액.
    @Transactional
    public int debit(
            Long userId,
            int amount,
            String type,
            String referenceType,
            Long referenceId,
            String activityTitle
    ) {
        if (userPointRepository.debitBalance(userId, amount) == 0) {
            throw new RewardException(RewardErrorCode.INSUFFICIENT_POINT);
        }
        UserPoint userPoint = userPointRepository.findById(userId).orElseThrow();
        PointTransaction transaction = pointTransactionRepository.save(
                PointTransaction.spend(userId, amount, type, referenceType, referenceId, userPoint.getBalance())
        );
        rewardActivityRepository.save(
                RewardActivity.of(userId, ActivityType.POINT_SPENT, activityTitle, transaction, referenceType, referenceId)
        );
        return userPoint.getBalance();
    }
}
