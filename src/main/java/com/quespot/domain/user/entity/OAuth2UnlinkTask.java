package com.quespot.domain.user.entity;

import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.OAuth2UnlinkTaskStatus;
import com.quespot.domain.user.service.OAuth2UnlinkCommand;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth2_unlink_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuth2UnlinkTask extends BaseEntity {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final long MAX_RETRY_DELAY_SECONDS = 3600;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private LoginProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OAuth2UnlinkTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    private OAuth2UnlinkTask(UserSocialAccount account) {
        this.provider = account.getProvider();
        this.providerUserId = account.getProviderUserId();
        this.encryptedAccessToken = account.getEncryptedAccessToken();
        this.encryptedRefreshToken = account.getEncryptedRefreshToken();
        this.status = OAuth2UnlinkTaskStatus.PENDING;
        this.nextAttemptAt = LocalDateTime.now();
    }

    public static OAuth2UnlinkTask create(UserSocialAccount account) {
        return new OAuth2UnlinkTask(account);
    }

    public OAuth2UnlinkCommand toCommand() {
        return new OAuth2UnlinkCommand(
                provider,
                providerUserId,
                encryptedAccessToken,
                encryptedRefreshToken
        );
    }

    public void recordFailure(String errorMessage) {
        this.attemptCount++;
        long retryDelaySeconds = Math.min(
                30L * (1L << Math.min(attemptCount - 1, 7)),
                MAX_RETRY_DELAY_SECONDS
        );
        this.nextAttemptAt = LocalDateTime.now().plusSeconds(retryDelaySeconds);
        this.lastError = truncate(errorMessage);
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
