package com.quespot.domain.user.converter;

import com.quespot.domain.user.dto.res.LoginResponseDTO;
import com.quespot.domain.user.dto.res.SendEmailVerificationCodeResponseDTO;
import com.quespot.domain.user.dto.res.SignUpResponseDTO;
import com.quespot.domain.user.dto.res.TokenReissueResponseDTO;
import com.quespot.domain.user.dto.res.VerifyEmailCodeResponseDTO;
import com.quespot.domain.user.dto.token.LoginResultDTO;
import com.quespot.domain.user.dto.token.TokenReissueResultDTO;
import com.quespot.domain.user.entity.User;
import com.quespot.domain.user.enums.TravelStyle;
import com.quespot.global.security.provider.JwtTokenPair;

import java.util.Comparator;

public final class AuthConverter {

    private AuthConverter() {
    }

    public static SignUpResponseDTO toSignUpResponseDTO(User user) {
        return new SignUpResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getTravelStyles().stream()
                        .sorted(Comparator.comparingInt(TravelStyle::ordinal))
                        .toList()
        );
    }

    public static SendEmailVerificationCodeResponseDTO toSendEmailVerificationCodeResponseDTO(
            String email,
            Integer expiresInMinutes
    ) {
        return new SendEmailVerificationCodeResponseDTO(email, expiresInMinutes);
    }

    public static VerifyEmailCodeResponseDTO toVerifyEmailCodeResponseDTO(String email) {
        return new VerifyEmailCodeResponseDTO(email, true);
    }

    public static LoginResultDTO toLoginResultDTO(User user, JwtTokenPair tokenPair) {
        return new LoginResultDTO(
                new LoginResponseDTO(user.getId(), tokenPair.accessToken()),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresInSeconds()
        );
    }

    public static TokenReissueResultDTO toTokenReissueResultDTO(JwtTokenPair tokenPair) {
        return new TokenReissueResultDTO(
                new TokenReissueResponseDTO(tokenPair.accessToken()),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresInSeconds()
        );
    }
}
