package com.quespot.domain.user.entity;

import com.quespot.domain.user.enums.LoginProvider;
import com.quespot.domain.user.enums.TravelStyle;
import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    public static final String DEFAULT_PROFILE_IMAGE_URL =
            "https://example.com/images/default-profile.png";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nickname", nullable = false, length = 10)
    private String nickname;

    @Column(name = "profile_image_url", nullable = false, length = 2048)
    private String profileImageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_travel_styles",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_user_travel_styles_user_id_travel_style",
                            columnNames = {"user_id", "travel_style"}
                    )
            }
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style", nullable = false, length = 30)
    private Set<TravelStyle> travelStyles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private LoginProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private User(
            String email,
            String password,
            String nickname,
            String profileImageUrl,
            Set<TravelStyle> travelStyles,
            LoginProvider provider,
            UserRole role,
            UserStatus status
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = resolveProfileImageUrl(profileImageUrl);
        this.travelStyles.addAll(resolveTravelStyles(travelStyles));
        this.provider = provider;
        this.role = role;
        this.status = status;
    }

    public static User createEmailUser(String email, String encodedPassword, String nickname) {
        return createEmailUser(email, encodedPassword, nickname, null, Set.of());
    }

    public static User createEmailUser(
            String email,
            String encodedPassword,
            String nickname,
            String profileImageUrl,
            Set<TravelStyle> travelStyles
    ) {
        return new User(
                email,
                encodedPassword,
                nickname,
                profileImageUrl,
                travelStyles,
                LoginProvider.EMAIL,
                UserRole.USER,
                UserStatus.ACTIVE
        );
    }

    public void updateProfile(
            String nickname,
            String profileImageUrl,
            Set<TravelStyle> travelStyles
    ) {
        if (nickname != null) {
            this.nickname = nickname.trim();
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = resolveProfileImageUrl(profileImageUrl);
        }
        if (travelStyles != null) {
            this.travelStyles.clear();
            this.travelStyles.addAll(travelStyles);
        }
    }

    public void withdraw() {
        this.email = "withdrawn_%d@deleted.quespot".formatted(id);
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }

    public String getProfileImageUrl() {
        return resolveProfileImageUrl(profileImageUrl);
    }

    private static String resolveProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return DEFAULT_PROFILE_IMAGE_URL;
        }
        return profileImageUrl.trim();
    }

    private static Set<TravelStyle> resolveTravelStyles(Set<TravelStyle> travelStyles) {
        return travelStyles == null ? Set.of() : travelStyles;
    }
}
