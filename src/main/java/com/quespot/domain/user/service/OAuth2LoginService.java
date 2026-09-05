package com.quespot.domain.user.service;

import com.quespot.domain.user.converter.AuthConverter;
import com.quespot.domain.user.dto.token.LoginResultDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.entity.UserSocialAccount;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.UserProfileRepository;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.domain.user.repository.UserSocialAccountRepository;
import com.quespot.global.security.provider.JwtTokenPair;
import com.quespot.global.security.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OAuth2LoginService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final OAuth2LoginCodeService oAuth2LoginCodeService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // 소셜 로그인 사용자 확인 및 일회용 코드 발급 로직
    @Transactional
    public String prepareLogin(String registrationId, OAuth2User oAuth2User) {
        LoginProvider provider = resolveProvider(registrationId);
        OAuth2UserInfo userInfo = resolveUserInfo(provider, oAuth2User);

        User user = userSocialAccountRepository
                .findByProviderAndProviderUserId(provider, userInfo.providerUserId())
                .map(UserSocialAccount::getUser)
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseGet(() -> createSocialUser(provider, userInfo));

        return oAuth2LoginCodeService.issue(user.getId());
    }

    // 일회용 코드를 Quespot 토큰으로 교환하는 로직
    @Transactional
    public LoginResultDTO exchangeLoginCode(String code) {
        Long userId = oAuth2LoginCodeService.consume(code);
        User user = userRepository.findByIdForUpdate(userId)
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_OAUTH2_LOGIN_CODE));

        JwtTokenPair tokenPair = jwtTokenProvider.issueTokenPair(user.getId(), user.getRole());
        refreshTokenService.saveRefreshToken(user.getId(), tokenPair);

        return AuthConverter.toLoginResultDTO(
                user,
                tokenPair,
                userProfileRepository.existsByUserId(user.getId())
        );
    }

    private User createSocialUser(LoginProvider provider, OAuth2UserInfo userInfo) {
        if (userRepository.findByEmailForUpdate(userInfo.email()).isPresent()) {
            throw new AuthException(AuthErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
        }

        try {
            User user = userRepository.saveAndFlush(
                    User.createSocialUser(userInfo.email(), provider)
            );
            userSocialAccountRepository.saveAndFlush(
                    UserSocialAccount.create(user, provider, userInfo.providerUserId())
            );
            return user;
        } catch (DataIntegrityViolationException exception) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }
    }

    private LoginProvider resolveProvider(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }

        try {
            LoginProvider provider = LoginProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
            if (provider == LoginProvider.EMAIL) {
                throw new IllegalArgumentException();
            }
            return provider;
        } catch (IllegalArgumentException exception) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }
    }

    private OAuth2UserInfo resolveUserInfo(LoginProvider provider, OAuth2User oAuth2User) {
        return switch (provider) {
            case GOOGLE -> resolveGoogleUserInfo(oAuth2User);
            default -> throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        };
    }

    private OAuth2UserInfo resolveGoogleUserInfo(OAuth2User oAuth2User) {
        if (!Boolean.TRUE.equals(oAuth2User.getAttribute("email_verified"))) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }

        return new OAuth2UserInfo(
                requireAttribute(oAuth2User, "sub"),
                normalizeEmail(requireAttribute(oAuth2User, "email"))
        );
    }

    private String requireAttribute(OAuth2User oAuth2User, String attributeName) {
        Object attribute = oAuth2User.getAttribute(attributeName);
        if (!(attribute instanceof String value) || value.isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH2_LOGIN_FAILED);
        }
        return value;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record OAuth2UserInfo(String providerUserId, String email) {
    }
}
