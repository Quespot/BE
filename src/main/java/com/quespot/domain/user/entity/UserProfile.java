package com.quespot.domain.user.entity;

import com.quespot.domain.user.enums.Gender;
import com.quespot.domain.user.enums.TravelStyle;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "user_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_profiles_user_id", columnNames = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    public static final String DEFAULT_PROFILE_IMAGE_URL =
            "https://example.com/images/default-profile.png";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "nickname", nullable = false, length = 10)
    private String nickname;

    @Column(name = "profile_image_url", nullable = false, length = 2048)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_profile_travel_styles",
            joinColumns = @JoinColumn(name = "profile_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_profile_travel_styles_profile_id_travel_style",
                            columnNames = {"profile_id", "travel_style"}
                    )
            }
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_style", nullable = false, length = 30)
    private Set<TravelStyle> travelStyles = new HashSet<>();

    private UserProfile(
            User user,
            String nickname,
            String profileImageUrl,
            Gender gender,
            LocalDate birthDate,
            Set<TravelStyle> travelStyles
    ) {
        this.user = user;
        this.nickname = nickname;
        this.profileImageUrl = resolveProfileImageUrl(profileImageUrl);
        this.gender = gender;
        this.birthDate = birthDate;
        this.travelStyles.addAll(resolveTravelStyles(travelStyles));
    }

    public static UserProfile create(
            User user,
            String nickname,
            String profileImageUrl,
            Gender gender,
            LocalDate birthDate,
            Set<TravelStyle> travelStyles
    ) {
        return new UserProfile(user, nickname, profileImageUrl, gender, birthDate, travelStyles);
    }

    public void update(
            String nickname,
            String profileImageUrl,
            Gender gender,
            LocalDate birthDate,
            Set<TravelStyle> travelStyles
    ) {
        if (nickname != null) {
            this.nickname = nickname.trim();
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = resolveProfileImageUrl(profileImageUrl);
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (travelStyles != null) {
            this.travelStyles.clear();
            this.travelStyles.addAll(travelStyles);
        }
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
