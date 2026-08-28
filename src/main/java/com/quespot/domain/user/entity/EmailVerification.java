package com.quespot.domain.user.entity;

import com.quespot.domain.user.enums.EmailVerificationPurpose;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verifications",
        indexes = {
                @Index(name = "idx_email_verifications_email", columnList = "email"),
                @Index(
                        name = "idx_email_verifications_rate_limit",
                        columnList = "email, client_key, purpose, created_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "client_key", nullable = false, length = 100)
    private String clientKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private EmailVerificationPurpose purpose;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "invalidated", nullable = false)
    private Boolean invalidated;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    private EmailVerification(
            String email,
            String codeHash,
            String clientKey,
            EmailVerificationPurpose purpose,
            LocalDateTime expiresAt
    ) {
        this.email = email;
        this.codeHash = codeHash;
        this.clientKey = clientKey;
        this.purpose = purpose;
        this.verified = false;
        this.invalidated = false;
        this.failedAttempts = 0;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification create(
            String email,
            String codeHash,
            String clientKey,
            EmailVerificationPurpose purpose,
            LocalDateTime expiresAt
    ) {
        return new EmailVerification(email, codeHash, clientKey, purpose, expiresAt);
    }

    public boolean canVerify(String codeHash, LocalDateTime now) {
        return !verified && !invalidated && this.codeHash.equals(codeHash) && expiresAt.isAfter(now);
    }

    public boolean isAttemptLimitExceeded(int failedAttemptLimit) {
        return invalidated || failedAttempts >= failedAttemptLimit;
    }

    public boolean recordFailedAttempt(int failedAttemptLimit) {
        this.failedAttempts++;

        if (failedAttempts >= failedAttemptLimit) {
            this.invalidated = true;
            return true;
        }

        return false;
    }

    public void verify(LocalDateTime verifiedAt) {
        this.verified = true;
        this.verifiedAt = verifiedAt;
    }
}
