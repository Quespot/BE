package com.quespot.domain.user.entity;

import com.quespot.domain.user.enums.Gender;
import com.quespot.domain.user.enums.ResidenceRegion;
import com.quespot.domain.user.enums.TravelCompanion;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "nickname", nullable = false, length = 10)
    private String nickname;

    @Column(name = "profile_image_object_key", length = 1024)
    private String profileImageObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "residence_region", nullable = false, length = 30)
    private ResidenceRegion residenceRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_companion", length = 20)
    private TravelCompanion travelCompanion;

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
            String profileImageObjectKey,
            Gender gender,
            LocalDate birthDate,
            ResidenceRegion residenceRegion,
            TravelCompanion travelCompanion,
            Set<TravelStyle> travelStyles
    ) {
        this.user = user;
        this.nickname = nickname;
        this.profileImageObjectKey = profileImageObjectKey;
        this.gender = gender;
        this.birthDate = birthDate;
        this.residenceRegion = residenceRegion;
        this.travelCompanion = travelCompanion;
        this.travelStyles.addAll(resolveTravelStyles(travelStyles));
    }

    public static UserProfile create(
            User user,
            String nickname,
            String profileImageObjectKey,
            Gender gender,
            LocalDate birthDate,
            ResidenceRegion residenceRegion,
            TravelCompanion travelCompanion,
            Set<TravelStyle> travelStyles
    ) {
        return new UserProfile(
                user,
                nickname,
                profileImageObjectKey,
                gender,
                birthDate,
                residenceRegion,
                travelCompanion,
                travelStyles
        );
    }

    public void update(
            String nickname,
            Gender gender,
            LocalDate birthDate,
            ResidenceRegion residenceRegion,
            TravelCompanion travelCompanion,
            Set<TravelStyle> travelStyles
    ) {
        if (nickname != null) {
            this.nickname = nickname.trim();
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (residenceRegion != null) {
            this.residenceRegion = residenceRegion;
        }
        if (travelCompanion != null) {
            this.travelCompanion = travelCompanion;
        }
        if (travelStyles != null) {
            this.travelStyles.clear();
            this.travelStyles.addAll(travelStyles);
        }
    }

    public void updateProfileImageObjectKey(String profileImageObjectKey) {
        this.profileImageObjectKey = profileImageObjectKey;
    }

    private static Set<TravelStyle> resolveTravelStyles(Set<TravelStyle> travelStyles) {
        return travelStyles == null ? Set.of() : travelStyles;
    }
}
