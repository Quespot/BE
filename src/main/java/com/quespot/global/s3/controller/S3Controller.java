package com.quespot.global.s3.controller;

import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.s3.dto.req.CreatePresignedUploadRequestDTO;
import com.quespot.global.s3.dto.res.PresignedUploadResponseDTO;
import com.quespot.global.s3.exception.code.S3SuccessCode;
import com.quespot.global.s3.service.PresignedUploadResult;
import com.quespot.global.s3.service.S3Service;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
@Tag(name = "S3", description = "파일 업로드 API")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    @Operation(
            summary = "S3 Presigned 업로드 URL 발급",
            description = """
                    파일 업로드는 3단계로 이루어진다.
                    1) 이 API로 presigned URL을 발급받는다.
                    2) 응답의 uploadUrl로 파일을 S3에 직접 PUT한다(requiredHeaders를 그대로 포함).
                    3) 업로드가 끝나면 최종 목적지 API(예: 미션 사진 등록)에 imageUrl로
                       "https://{bucket}.s3.{region}.amazonaws.com/{objectKey}" 형태의 정규 URL을 제출한다.
                       objectKey는 이 API 응답의 objectKey 값을 그대로 쓴다.
                    """
    )
    public ApiResponse<PresignedUploadResponseDTO> createPresignedUploadUrl(
            @Valid @RequestBody CreatePresignedUploadRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        PresignedUploadResult result = s3Service.createPresignedUploadUrl(
                principal.userId(),
                request.purpose(),
                request.originalFilename(),
                request.contentType(),
                request.fileSize()
        );
        return ApiResponse.of(
                S3SuccessCode.PRESIGNED_UPLOAD_URL_CREATED,
                PresignedUploadResponseDTO.from(result)
        );
    }
}
