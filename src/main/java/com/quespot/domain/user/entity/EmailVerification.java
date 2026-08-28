package com.quespot.domain.user.entity;

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

@Getter
@Entity
@Table(
        name = "email_verifications",
        indexes = {
                @Index(name = "idx_email_verifications_email", columnList = "email")
        }
)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private EmailVerificationPurpose purpose;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    private EmailVerification(
            String email,
            String codeHash,
            EmailVerificationPurpose purpose,
            LocalDateTime expiresAt
    ) {
        this.email = email;
        this.codeHash = codeHash;
        this.purpose = purpose;
        this.verified = false;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification create(
            String email,
            String codeHash,
            EmailVerificationPurpose purpose,
            LocalDateTime expiresAt
    ) {
        return new EmailVerification(email, codeHash, purpose, expiresAt);
    }

    public boolean canVerify(String codeHash, LocalDateTime now) {
        return !verified && this.codeHash.equals(codeHash) && expiresAt.isAfter(now);
    }

    public void verify(LocalDateTime verifiedAt) {
        this.verified = true;
        this.verifiedAt = verifiedAt;
    }
}
