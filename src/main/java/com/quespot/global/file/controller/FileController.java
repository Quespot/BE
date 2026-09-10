package com.quespot.global.file.controller;

import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.file.dto.req.CreatePresignedUploadRequestDTO;
import com.quespot.global.file.dto.res.PresignedUploadResponseDTO;
import com.quespot.global.file.exception.code.FileSuccessCode;
import com.quespot.global.file.model.PresignedUploadResult;
import com.quespot.global.file.service.FileService;
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
@RequestMapping("/api/files")
@Tag(name = "File", description = "파일 API")
public class FileController {

    private final FileService fileService;

    @PostMapping("/presigned-upload-url")
    @Operation(summary = "파일 업로드용 Presigned URL 발급")
    public ApiResponse<PresignedUploadResponseDTO> createPresignedUploadUrl(
            @Valid @RequestBody CreatePresignedUploadRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        PresignedUploadResult result = fileService.createPresignedUploadUrl(
                principal.userId(),
                request.purpose(),
                request.originalFilename(),
                request.contentType(),
                request.fileSize()
        );
        return ApiResponse.of(
                FileSuccessCode.PRESIGNED_UPLOAD_URL_CREATED,
                PresignedUploadResponseDTO.from(result)
        );
    }
}
