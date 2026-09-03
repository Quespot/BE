package com.quespot.domain.reward.service;

import com.quespot.domain.reward.converter.RewardConverter;
import com.quespot.domain.reward.dto.res.RewardActivityListResponseDTO;
import com.quespot.domain.reward.dto.res.RewardActivityResponseDTO;
import com.quespot.domain.reward.entity.RewardActivity;
import com.quespot.domain.reward.repository.RewardActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardActivityService {

    private final RewardActivityRepository rewardActivityRepository;

    @Transactional(readOnly = true)
    public RewardActivityListResponseDTO getActivities(Long userId, Long cursor, int size) {
        List<RewardActivity> fetched =
                rewardActivityRepository.findFeed(userId, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = fetched.size() > size;
        List<RewardActivity> page = hasNext ? fetched.subList(0, size) : fetched;

        List<RewardActivityResponseDTO> activities = page.stream()
                .map(RewardConverter::toRewardActivityResponseDTO)
                .toList();

        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;

        return new RewardActivityListResponseDTO(activities, nextCursor, hasNext);
    }
}
