package com.quespot.domain.user.service;

import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.entity.UserSocialAccount;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.domain.user.repository.UserSocialAccountRepository;
import com.quespot.global.security.principal.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginMethodServiceTest {

    private static final Long USER_ID = 1L;
    private static final AuthenticatedUser AUTHENTICATED_USER =
            new AuthenticatedUser(USER_ID, UserRole.USER, "session-id");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;

    @Mock
    private OAuth2LinkRequestService oAuth2LinkRequestService;

    @Mock
    private OAuth2ProviderUnlinkService oAuth2ProviderUnlinkService;

    @InjectMocks
    private LoginMethodService loginMethodService;

    @Test
    void returnsEmailAndAllSocialProviders() {
        User user = User.createEmailUser("member@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSocialAccountRepository.findAllByUserId(user.getId())).thenReturn(List.of());

        var response = loginMethodService.getLoginMethods(AUTHENTICATED_USER);

        assertThat(response.loginMethods()).hasSize(LoginProvider.values().length);
        assertThat(response.loginMethods())
                .filteredOn(method -> method.provider() == LoginProvider.EMAIL)
                .singleElement()
                .satisfies(method -> {
                    assertThat(method.linked()).isTrue();
                    assertThat(method.maskedEmail()).isEqualTo("me***@example.com");
                    assertThat(method.canUnlink()).isFalse();
                });
    }

    @Test
    void rejectsUnlinkingTheLastLoginMethod() {
        User user = User.createSocialUser("member@example.com", LoginProvider.NAVER);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        UserSocialAccount account = UserSocialAccount.create(
                user,
                LoginProvider.NAVER,
                "naver-user-id",
                "member@example.com",
                "encrypted-access-token",
                "encrypted-refresh-token",
                null
        );
        when(oAuth2LinkRequestService.resolveSocialProvider("naver"))
                .thenReturn(LoginProvider.NAVER);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userSocialAccountRepository.findByUserIdAndProvider(USER_ID, LoginProvider.NAVER))
                .thenReturn(Optional.of(account));
        when(userSocialAccountRepository.countByUserId(USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> loginMethodService.unlink(AUTHENTICATED_USER, "naver"))
                .isInstanceOfSatisfying(AuthException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AuthErrorCode.LAST_LOGIN_METHOD_CANNOT_BE_UNLINKED)
                );
        verify(userSocialAccountRepository, never()).delete(account);
        verify(oAuth2ProviderUnlinkService, never()).unlink(account);
    }

    @Test
    void unlinksSocialAccountWhenEmailLoginRemains() {
        User user = User.createEmailUser("member@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        UserSocialAccount account = UserSocialAccount.create(
                user,
                LoginProvider.NAVER,
                "naver-user-id",
                "member@naver.com",
                "encrypted-access-token",
                "encrypted-refresh-token",
                null
        );
        when(oAuth2LinkRequestService.resolveSocialProvider("naver"))
                .thenReturn(LoginProvider.NAVER);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userSocialAccountRepository.findByUserIdAndProvider(USER_ID, LoginProvider.NAVER))
                .thenReturn(Optional.of(account));
        when(userSocialAccountRepository.countByUserId(USER_ID)).thenReturn(1L);

        loginMethodService.unlink(AUTHENTICATED_USER, "naver");

        verify(oAuth2ProviderUnlinkService).unlink(account);
        verify(userSocialAccountRepository).delete(account);
    }
}
