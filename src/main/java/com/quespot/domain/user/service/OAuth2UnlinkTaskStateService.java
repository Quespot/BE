package com.quespot.domain.user.service;

import com.quespot.domain.user.repository.OAuth2UnlinkTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2UnlinkTaskStateService {

    private final OAuth2UnlinkTaskRepository oAuth2UnlinkTaskRepository;

    @Transactional
    public void complete(Long taskId) {
        oAuth2UnlinkTaskRepository.deleteById(taskId);
    }

    @Transactional
    public void fail(Long taskId, String errorMessage) {
        oAuth2UnlinkTaskRepository.findById(taskId)
                .ifPresent(task -> task.recordFailure(errorMessage));
    }
}
