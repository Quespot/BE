package com.quespot.domain.user.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequestDTO(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "비밀번호는 영문과 숫자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호 확인은 8자 이상 64자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "비밀번호 확인은 영문과 숫자를 포함해야 합니다."
        )
        String passwordConfirm
) {
}
