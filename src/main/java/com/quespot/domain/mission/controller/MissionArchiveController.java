package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.req.RegisterArchivePhotoRequestDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveItemResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionArchiveMapResponseDTO;
import com.quespot.domain.mission.entity.ArchivePhoto;
import com.quespot.domain.mission.enums.MissionCategory;
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
import jakarta.validation.constraints.Pattern;
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
@Tag(name = "MissionArchive", description = "미션 아카이브 API")
public class MissionArchiveController {

    private final MissionArchiveQueryService missionArchiveQueryService;
    private final ArchivePhotoService archivePhotoService;

    @GetMapping("/api/users/me/archives")
    @Operation(
            summary = "아카이브 피드 조회",
            description = """
                    미션 완료 후 등록한 사진(source=MISSION)과 미션과 무관하게 POST /api/users/me/archives로 자유 업로드한
                    사진(source=ARCHIVE)을 하나의 최신순 피드로 합쳐 돌려준다. source=ARCHIVE면 missionId·missionTitle·
                    missionCategory·completedAt이 null이다. photoId는 source별로 다른 테이블의 id라 겹칠 수 있으니
                    항목 식별은 source + photoId로 한다.

                    커서 페이징. 첫 요청은 cursor 없이, 응답의 nextCursor를 다음 요청의 cursor에 그대로 넣는다. hasNext=false면
                    마지막 페이지이고 nextCursor는 null. 결과가 없으면 archives는 빈 배열이다. cursor를 만들거나 파싱하지 말 것.
                    imageUrl은 매번 새로 서명한 presigned URL이라 저장하지 말고 조회 때마다 새로 받는다. 지도가 아니라 목록이라
                    좌표는 응답에 없다.
                    """
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

    @GetMapping("/api/users/me/archives/map")
    @Operation(
            summary = "지도형 아카이브 조회",
            description = """
                    완료한 미션을 지도 표시용 데이터로 조회한다. yearMonth(YYYY-MM), regionCode(2자리),
                    category는 모두 선택이며 함께 보내면 AND 조건으로 적용된다. footprint는 같은 조건으로
                    조회된 completedMissions의 개수와 실제 획득 포인트 합계다. spotName은 별도 스냅샷을
                    만들지 않고 미션 발행 시 저장된 Mission.snapshotName 값을 반환한다.
                    """
    )
    public ApiResponse<MissionArchiveMapResponseDTO> getMapArchive(
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "yearMonth는 YYYY-MM 형식이어야 합니다.")
            String yearMonth,
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{2}", message = "regionCode는 2자리 숫자여야 합니다.")
            String regionCode,
            @RequestParam(required = false) MissionCategory category,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.ARCHIVE_MAP_FOUND,
                missionArchiveQueryService.getMapArchive(
                        principal.userId(), yearMonth, regionCode, category
                )
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
