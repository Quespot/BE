package com.quespot.domain.user.service;

import com.quespot.domain.user.entity.OAuth2UnlinkTask;
import com.quespot.domain.user.enums.OAuth2UnlinkTaskStatus;
import com.quespot.domain.user.repository.OAuth2UnlinkTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2UnlinkTaskProcessor {

    private final OAuth2UnlinkTaskRepository oAuth2UnlinkTaskRepository;
    private final OAuth2ProviderUnlinkService oAuth2ProviderUnlinkService;
    private final OAuth2UnlinkTaskStateService oAuth2UnlinkTaskStateService;

    // 커밋된 외부 OAuth 연동 해제 작업을 조회하고 실패 작업을 재시도하는 로직
    @Scheduled(fixedDelayString = "${app.oauth2.unlink-retry-delay-ms:30000}")
    public void processPendingTasks() {
        List<OAuth2UnlinkTask> tasks = oAuth2UnlinkTaskRepository
                .findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        OAuth2UnlinkTaskStatus.PENDING,
                        LocalDateTime.now()
                );
        tasks.forEach(this::process);
    }

    private void process(OAuth2UnlinkTask task) {
        try {
            oAuth2ProviderUnlinkService.unlink(task.toCommand());
            oAuth2UnlinkTaskStateService.complete(task.getId());
        } catch (RuntimeException exception) {
            oAuth2UnlinkTaskStateService.fail(task.getId(), exception.getMessage());
        }
    }
}
