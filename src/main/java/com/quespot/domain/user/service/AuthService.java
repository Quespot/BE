package com.quespot.domain.user.service;

import com.quespot.domain.user.converter.AuthConverter;
import com.quespot.domain.user.dto.req.SignUpRequestDTO;
import com.quespot.domain.user.dto.res.SignUpResponseDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.EmailVerificationPurpose;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.EmailVerificationRepository;
import com.quespot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 로직
    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO request) {
        String email = normalizeEmail(request.email());
        String nickname = request.nickname().trim();

        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        boolean emailVerified = emailVerificationRepository
                .existsByEmailAndPurposeAndVerifiedIsTrueAndExpiresAtAfter(
                        email,
                        EmailVerificationPurpose.SIGN_UP,
                        LocalDateTime.now()
                );

        if (!emailVerified) {
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
