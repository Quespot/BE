package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.req.RegisterArchivePhotoRequestDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.ArchivePhotoService;
import com.quespot.domain.mission.service.MissionArchiveQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "MissionArchive", description = "미션 사진 아카이브 API")
public class MissionArchiveController {

    private final MissionArchiveQueryService missionArchiveQueryService;
    private final ArchivePhotoService archivePhotoService;

    @GetMapping("/api/users/me/archives")
    @Operation(
            summary = "내 아카이브 피드 조회",
            description = "GPS 인증 후 등록한 미션 사진과 자유 업로드 사진을 하나의 최신순 피드로 조회한다. " +
                    "지도가 아니라 목록이라 좌표는 응답에 포함하지 않는다."
    )
    public ApiResponse<MissionArchiveListResponseDTO> getArchives(
            @RequestParam(required = false) @Size(max = 500) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.ARCHIVES_FOUND,
                missionArchiveQueryService.getArchives(principal.userId(), cursor, size)
        );
    }

    @PostMapping("/api/users/me/archives")
    @Operation(
            summary = "아카이브 자유 업로드",
            description = """
                    미션과 무관하게 사진을 아카이브에 바로 등록한다. 업로드 3단계 흐름의 마지막 단계 —
                    먼저 POST /api/files/presigned-upload-url(purpose=ARCHIVE)로 objectKey와 uploadUrl을
                    발급받아 파일을 직접 PUT한 뒤, 그 objectKey를 그대로 제출한다(저장소가 비공개라 URL이
                    아니라 objectKey를 제출한다). archives/ 아래이고 본인이 발급받은 objectKey가 아니면
                    거부된다. 같은 objectKey를 다시 제출하면(재시도 등) 새로 만들지 않고 기존 사진을 200으로 돌려준다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "FILE_400_004 objectKey 형식 오류 / FILE_400_005 archives/ 아래가 아님(purpose 불일치) / FILE_400_006 본인이 업로드한 파일이 아님 — 업로드 1단계부터 다시",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<MissionArchiveItemResponseDTO> registerArchivePhoto(
            @Valid @RequestBody RegisterArchivePhotoRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        ArchivePhoto photo = archivePhotoService.registerPhoto(principal.userId(), request.objectKey(), request.caption());
        String photoViewUrl = archivePhotoService.resolveViewUrl(photo);
        return ApiResponse.of(
                MissionSuccessCode.ARCHIVE_PHOTO_REGISTERED,
                MissionConverter.toArchiveItem(photo, photoViewUrl)
        );
    }
}
