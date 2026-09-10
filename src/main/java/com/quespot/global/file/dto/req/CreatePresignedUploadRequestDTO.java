package com.quespot.global.file.dto.req;

import com.quespot.global.file.enums.UploadPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePresignedUploadRequestDTO(
        @NotNull(message = "업로드 용도는 필수입니다.")
        UploadPurpose purpose,

        @NotBlank(message = "파일명은 필수입니다.")
        @Size(max = 255, message = "파일명은 255자 이하여야 합니다.")
        String originalFilename,

        @NotBlank(message = "콘텐츠 타입은 필수입니다.")
        String contentType,

        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        Long fileSize
) {
}
