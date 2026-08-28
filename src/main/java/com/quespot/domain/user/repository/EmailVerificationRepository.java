package com.quespot.domain.user.repository;

import com.quespot.domain.user.entity.EmailVerification;
import com.quespot.domain.user.enums.EmailVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    boolean existsByEmailAndPurposeAndVerifiedIsTrueAndExpiresAtAfter(
            String email,
            EmailVerificationPurpose purpose,
            LocalDateTime now
    );

    long countByEmailAndClientKeyAndPurposeAndCreatedAtAfter(
            String email,
            String clientKey,
            EmailVerificationPurpose purpose,
            LocalDateTime createdAt
    );

    Optional<EmailVerification> findTopByEmailAndClientKeyAndPurposeOrderByCreatedAtDesc(
            String email,
            String clientKey,
            EmailVerificationPurpose purpose
    );
}
