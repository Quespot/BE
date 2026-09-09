package com.quespot.domain.user.service;

import com.quespot.domain.user.dto.res.LoginMethodListResponseDTO;
import com.quespot.domain.user.dto.res.LoginMethodResponseDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.entity.UserSocialAccount;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.domain.user.repository.UserSocialAccountRepository;
import com.quespot.global.security.principal.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginMethodService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final OAuth2LinkRequestService oAuth2LinkRequestService;
    private final OAuth2UnlinkQueueService oAuth2UnlinkQueueService;

    // 현재 사용자의 이메일 및 소셜 로그인 수단 조회 로직
    @Transactional(readOnly = true)
    public LoginMethodListResponseDTO getLoginMethods(AuthenticatedUser authenticatedUser) {
        User user = getActiveUser(authenticatedUser.userId());
        List<UserSocialAccount> socialAccounts = userSocialAccountRepository.findAllByUserId(user.getId());
        Map<LoginProvider, UserSocialAccount> accountByProvider = new EnumMap<>(LoginProvider.class);
        socialAccounts.forEach(account -> accountByProvider.put(account.getProvider(), account));

        int loginMethodCount = (hasEmailLogin(user) ? 1 : 0) + socialAccounts.size();
        List<LoginMethodResponseDTO> loginMethods = Arrays.stream(LoginProvider.values())
                .map(provider -> toResponse(user, accountByProvider.get(provider), provider, loginMethodCount))
                .toList();
        return new LoginMethodListResponseDTO(loginMethods);
    }

    // 소셜 로그인 수단 연결을 시작하기 위한 일회성 요청 발급 로직
    public OAuth2LinkRequestService.IssuedLinkRequest startLink(
            AuthenticatedUser authenticatedUser,
            String provider
    ) {
        User user = getActiveUser(authenticatedUser.userId());
        LoginProvider loginProvider = oAuth2LinkRequestService.resolveSocialProvider(provider);
        if (userSocialAccountRepository.findByUserIdAndProvider(user.getId(), loginProvider).isPresent()) {
            throw new AuthException(AuthErrorCode.LOGIN_METHOD_ALREADY_LINKED);
        }
        return oAuth2LinkRequestService.issue(user.getId(), provider);
    }

    // 최소 하나의 로그인 수단을 보장하며 소셜 계정 연결을 해제하는 로직
    @Transactional
    public void unlink(AuthenticatedUser authenticatedUser, String providerValue) {
        LoginProvider provider = oAuth2LinkRequestService.resolveSocialProvider(providerValue);
        User user = userRepository.findByIdForUpdate(authenticatedUser.userId())
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHORIZED));
        UserSocialAccount socialAccount = userSocialAccountRepository
                .findByUserIdAndProvider(user.getId(), provider)
                .orElseThrow(() -> new AuthException(AuthErrorCode.LOGIN_METHOD_NOT_FOUND));

        long loginMethodCount = userSocialAccountRepository.countByUserId(user.getId());
        if (hasEmailLogin(user)) {
            loginMethodCount++;
        }
        if (loginMethodCount <= 1) {
            throw new AuthException(AuthErrorCode.LAST_LOGIN_METHOD_CANNOT_BE_UNLINKED);
        }

        oAuth2UnlinkQueueService.enqueue(socialAccount);
        userSocialAccountRepository.delete(socialAccount);
    }

    private User getActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHORIZED));
    }

    private LoginMethodResponseDTO toResponse(
            User user,
            UserSocialAccount socialAccount,
            LoginProvider provider,
            int loginMethodCount
    ) {
        if (provider == LoginProvider.EMAIL) {
            boolean linked = hasEmailLogin(user);
            return new LoginMethodResponseDTO(
                    provider,
                    linked,
                    linked ? maskEmail(user.getEmail()) : null,
                    linked ? user.getCreatedAt() : null,
                    false
            );
        }

        boolean linked = socialAccount != null;
        String providerEmail = linked ? socialAccount.getProviderEmail() : null;
        if (providerEmail == null && user.getProvider() == provider) {
            providerEmail = user.getEmail();
        }
        return new LoginMethodResponseDTO(
                provider,
                linked,
                linked ? maskEmail(providerEmail) : null,
                linked ? socialAccount.getCreatedAt() : null,
                linked && loginMethodCount > 1
        );
    }

    private boolean hasEmailLogin(User user) {
        return user.getPassword() != null && !user.getPassword().isBlank();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        int visibleLength = Math.min(2, localPart.length());
        return localPart.substring(0, visibleLength) + "***" + email.substring(atIndex);
    }
}
