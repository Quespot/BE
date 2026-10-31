package com.quespot.domain.user.entity;

import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_accounts_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_social_accounts_user_id_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private LoginProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "access_token_expires_at")
    private LocalDateTime accessTokenExpiresAt;

    private UserSocialAccount(
            User user,
            LoginProvider provider,
            String providerUserId,
            String providerEmail,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            LocalDateTime accessTokenExpiresAt
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public static UserSocialAccount create(
            User user,
            LoginProvider provider,
            String providerUserId,
            String providerEmail,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            LocalDateTime accessTokenExpiresAt
    ) {
        return new UserSocialAccount(
                user,
                provider,
                providerUserId,
                providerEmail,
                encryptedAccessToken,
                encryptedRefreshToken,
                accessTokenExpiresAt
        );
    }

    public void updateProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public void updateOAuth2Credentials(
            String encryptedAccessToken,
            String encryptedRefreshToken,
            LocalDateTime accessTokenExpiresAt
    ) {
        this.encryptedAccessToken = encryptedAccessToken;
        if (encryptedRefreshToken != null) {
            this.encryptedRefreshToken = encryptedRefreshToken;
        }
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }
}
