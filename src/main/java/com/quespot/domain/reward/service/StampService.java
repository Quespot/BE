package com.quespot.domain.reward.service;

import com.quespot.domain.reward.converter.RewardConverter;
import com.quespot.domain.reward.dto.res.StampListResponseDTO;
import com.quespot.domain.reward.entity.Stamp;
import com.quespot.domain.reward.entity.UserStamp;
import com.quespot.domain.reward.repository.StampRepository;
import com.quespot.domain.reward.repository.UserStampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StampService {

    private final StampRepository stampRepository;
    private final UserStampRepository userStampRepository;

    @Transactional(readOnly = true)
    public StampListResponseDTO getStamps(Long userId) {
        List<Stamp> stamps = stampRepository.findAllByOrderBySortOrderAsc();

        Map<Long, UserStamp> acquiredByStampId = userStampRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(us -> us.getStamp().getId(), Function.identity()));

        return new StampListResponseDTO(
                stamps.stream()
                        .map(stamp -> RewardConverter.toStampResponseDTO(stamp, acquiredByStampId.get(stamp.getId())))
                        .toList()
        );
    }
}
