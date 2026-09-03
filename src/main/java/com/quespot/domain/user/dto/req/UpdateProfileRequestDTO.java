package com.quespot.domain.user.dto.req;

import com.quespot.domain.user.enums.Gender;
import com.quespot.domain.user.enums.TravelStyle;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.Set;

public record UpdateProfileRequestDTO(
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$",
                message = "닉네임은 한글, 영문, 숫자와 공백만 사용할 수 있습니다."
        )
        String nickname,

        @URL(protocol = "https", message = "프로필 이미지 URL은 올바른 HTTPS URL이어야 합니다.")
        String profileImageUrl,

        Gender gender,

        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate,

        @Size(max = 6, message = "여행 스타일은 최대 6개까지 선택할 수 있습니다.")
        Set<TravelStyle> travelStyles
) {

    public UpdateProfileRequestDTO {
        if (profileImageUrl != null) {
            profileImageUrl = profileImageUrl.trim();
        }
    }
}
