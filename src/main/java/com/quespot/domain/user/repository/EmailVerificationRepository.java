package com.quespot.domain.user.repository;

import com.quespot.domain.user.entity.EmailVerification;
import com.quespot.domain.user.entity.EmailVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    boolean existsByEmailAndPurposeAndVerifiedIsTrueAndExpiresAtAfter(
            String email,
            EmailVerificationPurpose purpose,
            LocalDateTime now
    );

    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            EmailVerificationPurpose purpose
    );
}
