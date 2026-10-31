package com.quespot.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI(
            @Value("${app.swagger.local-server-url}") String localServerUrl,
            @Value("${app.swagger.production-server-url}") String productionServerUrl,
            Environment environment) {
        String securitySchemeName = "bearerAuth";
        Server localServer = new Server()
                .url(localServerUrl)
                .description("로컬 서버");
        Server productionServer = new Server()
                .url(productionServerUrl)
                .description("운영 서버");
        List<Server> servers = environment.acceptsProfiles(Profiles.of("prod"))
                ? List.of(productionServer, localServer)
                : List.of(localServer, productionServer);

        return new OpenAPI()
                .info(new Info()
                        .title("Quespot API")
                        .description("""
                                Quespot API 문서입니다.

                                그룹은 **화면 단위**로 나뉘어 있습니다. 화면 하나를 만들 때 그 그룹 하나만 열면 됩니다. \
                                한 API가 여러 화면에서 쓰이면 여러 그룹에 함께 나옵니다(예: 달성 현황 → 02 홈 + 06 보상 + 07 마이페이지).

                                에러 응답은 `{ isSuccess:false, code, message, errorDetail? }` 형태이며 `code`로 분기합니다. \
                                전체 에러 코드 표와 프론트 처리 제안은 \
                                [docs/error-codes.md](https://github.com/Quespot/BE/blob/develop/docs/error-codes.md)를 참고하세요.
                                """)
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .servers(servers);
    }

    // ── 그룹은 화면 단위(#52). 한 API가 여러 그룹에 있어도 된다(springdoc은 pathsToMatch 중복 허용).
    // 모든 컨트롤러 경로가 01~09 어딘가에 반드시 포함되는지는 SwaggerGroupCoverageTest가 00-all과 대조한다.
    // 새 컨트롤러를 만들면 여기 어느 그룹엔가 경로를 넣어야 그 테스트가 통과한다.

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("00-all")
                .displayName("00. 전체")
                .pathsToMatch("/api/**")
                .build();
    }

    /** 로그인·가입·토큰 재발급·탈퇴·프로필·소셜 연동. 프로필 이미지 업로드 1단계인 files 포함. */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("01-auth")
                .displayName("01. 인증·회원")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/users/me/profile/**",
                        "/api/users/me/login-methods/**",
                        "/api/files/**"
                )
                .build();
    }

    /** 홈 상단 숫자(달성 현황)와 퀘스티 렌더링. */
    @Bean
    public GroupedOpenApi homeApi() {
        return GroupedOpenApi.builder()
                .group("02-home")
                .displayName("02. 홈")
                .pathsToMatch(
                        "/api/users/me/achievements/**",
                        "/api/users/me/questy"
                )
                .build();
    }

    /** 미션 목록·상세·시작·GPS 인증·사진·아카이브. /api/missions/**가 좋아요·시작·잠금 조건도 잡는다. */
    @Bean
    public GroupedOpenApi missionApi() {
        return GroupedOpenApi.builder()
                .group("03-mission")
                .displayName("03. 미션")
                .pathsToMatch(
                        "/api/missions/**",
                        "/api/mission-attempts/**",
                        "/api/users/me/archives/**",
                        "/api/files/**"
                )
                .build();
    }

    /** 코스 생성·조회·진행·포기. 담당·화면이 미션과 달라 분리(#52). */
    @Bean
    public GroupedOpenApi courseApi() {
        return GroupedOpenApi.builder()
                .group("04-course")
                .displayName("04. 코스")
                .pathsToMatch(
                        "/api/mission-courses/**",
                        "/api/course-attempts/**",
                        "/api/missions/*/unlock-condition"
                )
                .build();
    }

    /** 행정구역별 미션 스팟. 스팟 검색·상세·길찾기(/api/spots)는 아직 없어 넣지 않는다. */
    @Bean
    public GroupedOpenApi mapApi() {
        return GroupedOpenApi.builder()
                .group("05-map")
                .displayName("05. 지도·장소")
                .pathsToMatch("/api/mission-spots/**")
                .build();
    }

    /** 포인트·활동 내역·배지·스탬프·달성 현황. */
    @Bean
    public GroupedOpenApi rewardApi() {
        return GroupedOpenApi.builder()
                .group("06-reward")
                .displayName("06. 보상")
                .pathsToMatch(
                        "/api/users/me/points/**",
                        "/api/users/me/reward-activities/**",
                        "/api/users/me/badges/**",
                        "/api/users/me/stamps/**",
                        "/api/users/me/achievements/**"
                )
                .build();
    }

    /** 마이페이지 화면이 부르는 전부: 프로필·달성 현황·퀘스티 꾸미기·아이템·상점·좋아요·아카이브. */
    @Bean
    public GroupedOpenApi myPageApi() {
        return GroupedOpenApi.builder()
                .group("07-mypage")
                .displayName("07. 마이페이지")
                .pathsToMatch(
                        "/api/users/me/profile/**",
                        "/api/users/me/achievements/**",
                        "/api/users/me/questy",
                        "/api/users/me/items/**",
                        "/api/shop/items/**",
                        "/api/missions/*/like",
                        "/api/mission-courses/*/like",
                        "/api/users/me/liked-missions",
                        "/api/users/me/liked-courses",
                        "/api/users/me/archives/**",
                        "/api/files/**"
                )
                .build();
    }

    /** FCM 토큰·알림 설정. */
    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("08-notification")
                .displayName("08. 알림")
                .pathsToMatch(
                        "/api/notifications/**",
                        "/api/users/me/notification-settings/**"
                )
                .build();
    }

    /** 미션 후보 검수·TourAPI 동기화. ADMIN 역할 필요. */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("09-admin")
                .displayName("09. 관리자")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
