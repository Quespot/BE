package com.quespot.domain.user.service;

import com.quespot.domain.user.entity.OAuth2UnlinkTask;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.entity.UserSocialAccount;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.repository.OAuth2UnlinkTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2UnlinkTaskProcessorTest {

    @Mock
    private OAuth2UnlinkTaskRepository oAuth2UnlinkTaskRepository;

    @Mock
    private OAuth2ProviderUnlinkService oAuth2ProviderUnlinkService;

    @Mock
    private OAuth2UnlinkTaskStateService oAuth2UnlinkTaskStateService;

    @InjectMocks
    private OAuth2UnlinkTaskProcessor processor;

    @Test
    void skipsTaskWhenClaimFails() {
        OAuth2UnlinkTask task = createTask();
        when(oAuth2UnlinkTaskRepository
                .findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        any(),
                        any()
                ))
                .thenReturn(List.of(task));
        when(oAuth2UnlinkTaskStateService.claim(LoginProvider.NAVER, "naver-user-id"))
                .thenReturn(Optional.empty());

        processor.processPendingTasks();

        verify(oAuth2ProviderUnlinkService, never()).unlink(any());
    }

    private OAuth2UnlinkTask createTask() {
        User user = User.createSocialUser("member@example.com", LoginProvider.NAVER);
        UserSocialAccount account = UserSocialAccount.create(
                user,
                LoginProvider.NAVER,
                "naver-user-id",
                "member@example.com",
                "encrypted-access-token",
                "encrypted-refresh-token",
                LocalDateTime.now().plusHours(1)
        );
        return OAuth2UnlinkTask.create(account);
    }
}
