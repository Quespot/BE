package com.quespot.domain.user.service;

import com.quespot.domain.user.dto.req.SendEmailVerificationCodeRequestDTO;
import com.quespot.domain.user.dto.req.VerifyEmailCodeRequestDTO;
import com.quespot.domain.user.dto.res.SendEmailVerificationCodeResponseDTO;
import com.quespot.domain.user.dto.res.VerifyEmailCodeResponseDTO;
import com.quespot.domain.user.entity.EmailVerification;
import com.quespot.domain.user.entity.EmailVerificationPurpose;
import com.quespot.domain.user.repository.EmailVerificationRepository;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.global.apiPayload.code.GeneralErrorCode;
import com.quespot.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender javaMailSender;

    @Value("${app.mail.verification-code-expiration-minutes}")
    private Integer verificationCodeExpirationMinutes;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.verification-code-secret}")
    private String verificationCodeSecret;

    @Transactional
    public SendEmailVerificationCodeResponseDTO sendVerificationCode(
            SendEmailVerificationCodeRequestDTO request
    ) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new GeneralException(GeneralErrorCode.AUTH_409_001);
        }

        String code = generateVerificationCode();
        String codeHash = hashVerificationCode(email, code);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(verificationCodeExpirationMinutes);

        emailVerificationRepository.save(EmailVerification.create(
                email,
                codeHash,
                EmailVerificationPurpose.SIGN_UP,
                expiresAt
        ));

        sendMail(email, code);

        return new SendEmailVerificationCodeResponseDTO(email, verificationCodeExpirationMinutes);
    }

    @Transactional
    public VerifyEmailCodeResponseDTO verifyEmailCode(VerifyEmailCodeRequestDTO request) {
        String email = normalizeEmail(request.email());
        String codeHash = hashVerificationCode(email, request.code());
        LocalDateTime now = LocalDateTime.now();

        EmailVerification emailVerification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EmailVerificationPurpose.SIGN_UP)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.AUTH_400_002));

        if (!emailVerification.canVerify(codeHash, now)) {
            throw new GeneralException(GeneralErrorCode.AUTH_400_002);
        }

        emailVerification.verify(now);

        return new VerifyEmailCodeResponseDTO(email, true);
    }

    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(email);
        message.setSubject("[Quespot] 이메일 인증 코드");
        message.setText("""
                Quespot 이메일 인증 코드입니다.

                인증 코드: %s
                유효 시간: %d분
                """.formatted(code, verificationCodeExpirationMinutes));

        javaMailSender.send(message);
    }

    private String generateVerificationCode() {
        return String.valueOf(SECURE_RANDOM.nextInt(900000) + 100000);
    }

    private String hashVerificationCode(String email, String code) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(
                    "%s:%s:%s".formatted(email, code, verificationCodeSecret)
                            .getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
