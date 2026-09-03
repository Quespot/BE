package com.quespot.domain.user.service;

import com.quespot.domain.user.converter.ProfileConverter;
import com.quespot.domain.user.dto.req.CreateProfileRequestDTO;
import com.quespot.domain.user.dto.req.UpdateProfileRequestDTO;
import com.quespot.domain.user.dto.res.ProfileResponseDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.entity.UserProfile;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.ProfileException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.exception.code.ProfileErrorCode;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.domain.user.repository.UserProfileRepository;
import com.quespot.global.security.principal.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final String defaultProfileImageUrl;

    public ProfileService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            @Value("${app.profile.default-image-url}") String defaultProfileImageUrl
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.defaultProfileImageUrl = defaultProfileImageUrl;
    }

    // 프로필 생성 로직
    @Transactional
    public ProfileResponseDTO createProfile(
            AuthenticatedUser authenticatedUser,
            CreateProfileRequestDTO request
    ) {
        User user = findActiveUserForUpdate(authenticatedUser);

        if (userProfileRepository.existsByUserId(user.getId())) {
            throw new ProfileException(ProfileErrorCode.PROFILE_ALREADY_EXISTS);
        }

        UserProfile profile = UserProfile.create(
                user,
                request.nickname().trim(),
                resolveProfileImageUrl(request.profileImageUrl()),
                request.gender(),
                request.birthDate(),
                request.travelStyles()
        );

        try {
            return ProfileConverter.toProfileResponseDTO(userProfileRepository.saveAndFlush(profile));
        } catch (DataIntegrityViolationException exception) {
            throw new ProfileException(ProfileErrorCode.PROFILE_ALREADY_EXISTS);
        }
    }

    // 프로필 조회 로직
    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfile(AuthenticatedUser authenticatedUser) {
        User user = findActiveUser(authenticatedUser);
        return ProfileConverter.toProfileResponseDTO(findProfile(user.getId()));
    }

    // 프로필 수정 로직
    @Transactional
    public ProfileResponseDTO updateProfile(
            AuthenticatedUser authenticatedUser,
            UpdateProfileRequestDTO request
    ) {
        User user = findActiveUser(authenticatedUser);
        UserProfile profile = findProfile(user.getId());
        profile.update(
                request.nickname(),
                request.profileImageUrl() == null
                        ? null
                        : resolveProfileImageUrl(request.profileImageUrl()),
                request.gender(),
                request.birthDate(),
                request.travelStyles()
        );
        return ProfileConverter.toProfileResponseDTO(profile);
    }

    private User findActiveUser(AuthenticatedUser authenticatedUser) {
        validateAuthenticatedUser(authenticatedUser);

        return userRepository.findById(authenticatedUser.userId())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN));
    }

    private User findActiveUserForUpdate(AuthenticatedUser authenticatedUser) {
        validateAuthenticatedUser(authenticatedUser);

        return userRepository.findByIdForUpdate(authenticatedUser.userId())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN));
    }

    private UserProfile findProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    private void validateAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }
    }

    private String resolveProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return defaultProfileImageUrl;
        }
        return profileImageUrl.trim();
    }
}
