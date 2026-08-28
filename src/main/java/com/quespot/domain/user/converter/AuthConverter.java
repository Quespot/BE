package com.quespot.domain.user.converter;

import com.quespot.domain.user.dto.res.SendEmailVerificationCodeResponseDTO;
import com.quespot.domain.user.dto.res.SignUpResponseDTO;
import com.quespot.domain.user.dto.res.VerifyEmailCodeResponseDTO;
import com.quespot.domain.user.entity.User;

public final class AuthConverter {

    private AuthConverter() {
    }

    public static SignUpResponseDTO toSignUpResponseDTO(User user) {
        return new SignUpResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getNickname()
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
}
