package com.quespot.domain.user.service;

import com.quespot.domain.user.converter.ProfileConverter;
import com.quespot.domain.user.dto.req.UpdateProfileRequestDTO;
import com.quespot.domain.user.dto.res.ProfileResponseDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.UserStatus;
import com.quespot.domain.user.exception.AuthException;
import com.quespot.domain.user.exception.code.AuthErrorCode;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.global.security.principal.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfile(AuthenticatedUser authenticatedUser) {
        return ProfileConverter.toProfileResponseDTO(findActiveUser(authenticatedUser));
    }

    @Transactional
    public ProfileResponseDTO updateProfile(
            AuthenticatedUser authenticatedUser,
            UpdateProfileRequestDTO request
    ) {
        User user = findActiveUser(authenticatedUser);
        user.updateProfile(request.nickname(), request.profileImageUrl(), request.travelStyles());
        return ProfileConverter.toProfileResponseDTO(user);
    }

    private User findActiveUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }

        return userRepository.findById(authenticatedUser.userId())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN));
    }
}
