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
                        .description("Quespot API 문서입니다.")
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

    /**
     * 전체 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("00-all")
                .displayName("00. 전체 API")
                .pathsToMatch("/api/**")
                .build();
    }

    /**
     * 회원, 인증 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("01-user")
                .displayName("01. 회원·인증 API")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/users/me/profile/**",
                        "/api/users/me/login-methods/**"
                )
                .build();
    }

    /**
     * 미션 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi missionApi() {
        return GroupedOpenApi.builder()
                .group("02-mission")
                .displayName("02. 미션 API")
                .pathsToMatch(
                        "/api/missions/**",
                        "/api/mission-spots/**",
                        "/api/mission-attempts/**",
                        "/api/mission-courses/**",
                        "/api/photo-verifications/**"
                )
                .build();
    }

    /**
     * 포인트, 활동 내역, 배지, 스탬프 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi rewardApi() {
        return GroupedOpenApi.builder()
                .group("03-reward")
                .displayName("03. 보상 API")
                .pathsToMatch(
                        "/api/users/me/points/**",
                        "/api/users/me/reward-activities/**",
                        "/api/users/me/badges/**",
                        "/api/users/me/stamps/**"
                )
                .build();
    }

    /**
     * 사용자 보유 아이템과 상점 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi itemApi() {
        return GroupedOpenApi.builder()
                .group("04-item")
                .displayName("04. 아이템·상점 API")
                .pathsToMatch(
                        "/api/users/me/items/**",
                        "/api/shop/items/**"
                )
                .build();
    }

    /**
     * 알림 설정과 FCM 토큰 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("05-notification")
                .displayName("05. 알림 API")
                .pathsToMatch(
                        "/api/users/me/notification-settings/**",
                        "/api/notifications/**"
                )
                .build();
    }

    /**
     * 관리자 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("06-admin")
                .displayName("06. 관리자 API")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    /**
     * 파일 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("07-file")
                .displayName("07. 파일 API")
                .pathsToMatch("/api/files/**")
                .build();
    }
}
