package com.quespot.domain.user.service;

import com.quespot.domain.user.dto.req.SignUpRequestDTO;
import com.quespot.domain.user.dto.res.SignUpResponseDTO;
import com.quespot.domain.user.entity.EmailVerificationPurpose;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.repository.EmailVerificationRepository;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.global.apiPayload.code.GeneralErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO request) {
        String email = normalizeEmail(request.email());
        String nickname = request.nickname().trim();

        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(GeneralErrorCode.AUTH_409_001);
        }

        boolean emailVerified = emailVerificationRepository
                .existsByEmailAndPurposeAndVerifiedIsTrueAndExpiresAtAfter(
                        email,
                        EmailVerificationPurpose.SIGN_UP,
                        LocalDateTime.now()
                );

        if (!emailVerified) {
            throw new GeneralException(GeneralErrorCode.AUTH_400_001);
        }

        User user = User.createEmailUser(
                email,
                passwordEncoder.encode(request.password()),
                nickname
        );

        return SignUpResponseDTO.from(userRepository.save(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
