package com.quespot.domain.user.service;

import com.quespot.domain.user.converter.AuthConverter;
import com.quespot.domain.user.dto.req.LoginRequestDTO;
import com.quespot.domain.user.dto.req.SignUpRequestDTO;
import com.quespot.domain.user.dto.res.SignUpResponseDTO;
import com.quespot.domain.user.dto.token.LoginResultDTO;
import com.quespot.domain.user.dto.token.TokenReissueResultDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.EmailVerificationPurpose;
import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.global.security.principal.AuthenticatedUser;
import com.quespot.global.security.provider.JwtTokenPair;
import com.quespot.global.security.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 로직
    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO request) {
        String email = normalizeEmail(request.email());
        String nickname = request.nickname().trim();

        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        if (!emailVerificationService.isEmailVerified(email, EmailVerificationPurpose.SIGN_UP)) {
            throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        User user = User.createEmailUser(
                email,
                passwordEncoder.encode(request.password()),
                nickname
        );

        try {
            return AuthConverter.toSignUpResponseDTO(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }
    }

    // 일반 로그인 로직
    public LoginResultDTO login(LoginRequestDTO request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .filter(this::isActiveEmailUser)
                .filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPassword()))
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_LOGIN_CREDENTIALS));

        JwtTokenPair tokenPair = jwtTokenProvider.issueTokenPair(user.getId(), user.getRole());
        refreshTokenService.saveRefreshToken(user.getId(), tokenPair);

        return AuthConverter.toLoginResultDTO(user, tokenPair);
    }

    // 토큰 재발급 로직
    public TokenReissueResultDTO reissueToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        AuthenticatedUser authenticatedUser = jwtTokenProvider.parseRefreshToken(refreshToken);
        User user = userRepository.findById(authenticatedUser.userId())
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        JwtTokenPair newTokenPair = jwtTokenProvider.issueTokenPair(
                user.getId(),
                user.getRole(),
                authenticatedUser.sessionId()
        );
        refreshTokenService.rotateRefreshToken(user.getId(), refreshToken, newTokenPair);

        return AuthConverter.toTokenReissueResultDTO(newTokenPair);
    }

    private boolean isActiveEmailUser(User user) {
        return user.getStatus() == UserStatus.ACTIVE && user.getProvider() == LoginProvider.EMAIL;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
