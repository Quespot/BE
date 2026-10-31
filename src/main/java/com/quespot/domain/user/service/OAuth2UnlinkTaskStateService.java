package com.quespot.domain.user.service;

import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.OAuth2UnlinkTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2UnlinkTaskStateService {

    private final OAuth2UnlinkTaskRepository oAuth2UnlinkTaskRepository;

    // OAuth 연동 해제 작업 선점 로직
    @Transactional
    public Optional<ClaimedUnlinkTask> claim(LoginProvider provider, String providerUserId) {
        return oAuth2UnlinkTaskRepository
                .findByProviderAndProviderUserIdForUpdate(provider, providerUserId)
                .filter(task -> task.claim(LocalDateTime.now()))
                .map(task -> new ClaimedUnlinkTask(task.getId(), task.toCommand()));
    }

    // OAuth 연동 해제 작업 완료 처리 로직
    @Transactional
    public void complete(Long taskId) {
        oAuth2UnlinkTaskRepository.findByIdForUpdate(taskId)
                .filter(task -> task.isProcessing())
                .ifPresent(oAuth2UnlinkTaskRepository::delete);
    }

    // OAuth 연동 해제 작업 실패 처리 로직
    @Transactional
    public void fail(Long taskId, String errorMessage, boolean retryable) {
        oAuth2UnlinkTaskRepository.findByIdForUpdate(taskId)
                .filter(task -> task.isProcessing())
                .ifPresent(task -> task.recordFailure(errorMessage, retryable));
    }

    // 소셜 계정 재연결 전 연동 해제 작업 정리 로직
    @Transactional
    public void prepareForRelink(LoginProvider provider, String providerUserId) {
        oAuth2UnlinkTaskRepository
                .findByProviderAndProviderUserIdForUpdate(provider, providerUserId)
                .ifPresent(task -> {
                    if (task.isProcessing()) {
                        throw new AuthException(AuthErrorCode.OAUTH2_UNLINK_FAILED);
                    }
                    oAuth2UnlinkTaskRepository.delete(task);
                });
    }

    public record ClaimedUnlinkTask(Long taskId, OAuth2UnlinkCommand command) {
    }
}
