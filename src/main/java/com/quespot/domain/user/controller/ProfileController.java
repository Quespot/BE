package com.quespot.domain.user.controller;

import com.quespot.domain.user.dto.req.UpdateProfileRequestDTO;
import com.quespot.domain.user.dto.res.ProfileResponseDTO;
import com.quespot.domain.user.exception.code.ProfileSuccessCode;
import com.quespot.domain.user.service.ProfileService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/profile")
@Tag(name = "Profile", description = "프로필 API")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "프로필 조회", description = "현재 로그인한 사용자의 프로필을 조회합니다.")
    public ApiResponse<ProfileResponseDTO> getProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.of(
                ProfileSuccessCode.PROFILE_FOUND,
                profileService.getProfile(authenticatedUser)
        );
    }

    @PatchMapping
    @Operation(
            summary = "프로필 수정",
            description = "닉네임, 프로필 이미지 URL, 여행 스타일 중 전달된 정보를 수정합니다. 빈 여행 스타일 목록은 전체 선택을 해제합니다."
    )
    public ApiResponse<ProfileResponseDTO> updateProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateProfileRequestDTO request
    ) {
        return ApiResponse.of(
                ProfileSuccessCode.PROFILE_UPDATED,
                profileService.updateProfile(authenticatedUser, request)
        );
    }
}
