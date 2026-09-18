package com.quespot.domain.user.service;

import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.entity.UserProfile;
import com.quespot.domain.user.enums.Gender;
import com.quespot.domain.user.enums.ResidenceRegion;
import com.quespot.domain.user.enums.UserRole;
import com.quespot.domain.user.repository.UserProfileRepository;
import com.quespot.domain.user.repository.UserRepository;
import com.quespot.global.file.service.FileService;
import com.quespot.global.security.principal.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Long USER_ID = 1L;
    private static final AuthenticatedUser AUTHENTICATED_USER =
            new AuthenticatedUser(USER_ID, UserRole.USER, "session-id");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void returnsNullImageUrlWhenProfileImageIsNotSet() {
        UserProfile profile = createProfile(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(profile.getUser()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        var response = profileService.getProfile(AUTHENTICATED_USER);

        assertThat(response.profileImageUrl()).isNull();
        verifyNoInteractions(fileService);
    }

    @Test
    void returnsPresignedImageUrlWhenProfileImageIsSet() {
        String objectKey = "profiles/1/profile.png";
        String presignedUrl = "https://example.com/presigned-profile.png";
        UserProfile profile = createProfile(objectKey);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(profile.getUser()));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(fileService.createPresignedDownloadUrl(objectKey)).thenReturn(presignedUrl);

        var response = profileService.getProfile(AUTHENTICATED_USER);

        assertThat(response.profileImageUrl()).isEqualTo(presignedUrl);
        verify(fileService).createPresignedDownloadUrl(objectKey);
    }

    private UserProfile createProfile(String objectKey) {
        User user = User.createEmailUser("member@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return UserProfile.create(
                user,
                "회원",
                objectKey,
                Gender.MALE,
                LocalDate.of(2000, 1, 1),
                ResidenceRegion.SEOUL,
                null,
                Set.of()
        );
    }
}
