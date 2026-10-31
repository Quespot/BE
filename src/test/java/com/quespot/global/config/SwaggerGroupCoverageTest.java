package com.quespot.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

// 모든 컨트롤러 경로가 화면 그룹(01~09) 어딘가에 반드시 들어가는지 00-all과 대조한다.
// 새 컨트롤러가 그룹에서 빠지면 CI에서 잡힌다(#52). 수동 대조를 코드로 고정한 것.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SwaggerGroupCoverageTest {

    private static final List<String> SCREEN_GROUPS = List.of(
            "01-auth", "02-home", "03-mission", "04-course", "05-map",
            "06-reward", "07-mypage", "08-notification", "09-admin"
    );

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("quespot_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("app.jwt.secret", () -> "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz");
        registry.add("app.mail.verification-code-secret", () -> "integration-test-mail-secret");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Set<String> pathsOf(String group) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs/" + group, String.class);
        assertThat(response.getStatusCode()).as("group %s should be served", group).isEqualTo(HttpStatus.OK);
        JsonNode paths = objectMapper.readTree(response.getBody()).path("paths");
        Set<String> result = new TreeSet<>();
        for (Iterator<String> it = paths.fieldNames(); it.hasNext(); ) {
            result.add(it.next());
        }
        return result;
    }

    @Test
    void everyPathInAllGroupBelongsToAtLeastOneScreenGroup() throws Exception {
        Set<String> all = pathsOf("00-all");
        assertThat(all).as("00-all should not be empty").isNotEmpty();

        Set<String> covered = new HashSet<>();
        for (String group : SCREEN_GROUPS) {
            Set<String> paths = pathsOf(group);
            assertThat(paths).as("group %s should match at least one path", group).isNotEmpty();
            covered.addAll(paths);
        }

        Set<String> missing = new TreeSet<>(all);
        missing.removeAll(covered);
        assertThat(missing).as("paths not covered by any screen group").isEmpty();
    }

    @Test
    void screenGroupsDoNotContainPathsOutsideAll() throws Exception {
        Set<String> all = pathsOf("00-all");
        for (String group : SCREEN_GROUPS) {
            assertThat(all).as("group %s leaks paths outside 00-all", group).containsAll(pathsOf(group));
        }
    }

    @Test
    void flowEndpointsCarryDocumentedErrorResponsesAndKeepSuccessResponse() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs/03-mission", String.class);
        JsonNode start = objectMapper.readTree(response.getBody())
                .path("paths").path("/api/missions/{missionId}/start").path("post").path("responses");

        assertThat(start.has("200")).as("success response must survive @ApiResponses").isTrue();
        assertThat(start.has("409")).isTrue();
        assertThat(start.path("409").path("description").asText()).contains("MISSION_409_011");
    }

    @Test
    void nullableFieldsAndEnumMeaningsAreRenderedInSchemas() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs/03-mission", String.class);
        JsonNode schemas = objectMapper.readTree(response.getBody()).path("components").path("schemas");

        // OpenAPI 3.0은 nullable:true, 3.1(springdoc 기본)은 type:["integer","null"]로 렌더링한다.
        JsonNode earnedPoint = schemas.path("ArrivalResponseDTO").path("properties").path("earnedPoint");
        boolean nullableFlag = earnedPoint.path("nullable").asBoolean(false);
        boolean nullType = false;
        for (JsonNode t : earnedPoint.path("type")) {
            nullType |= "null".equals(t.asText());
        }
        assertThat(nullableFlag || nullType).as("@Schema(nullable=true) must render; got %s", earnedPoint).isTrue();
        assertThat(earnedPoint.path("description").asText()).contains("null");

        // enum은 별도 컴포넌트가 아니라 프로퍼티에 인라인된다 — 타입 레벨 @Schema가 프로퍼티까지 오는지 확인
        JsonNode attemptStatus = schemas.path("MissionAttemptResponseDTO").path("properties").path("status");
        assertThat(attemptStatus.path("description").asText())
                .as("enum type-level @Schema should surface on property; got %s / components has UserMissionStatus=%s",
                        attemptStatus, schemas.has("UserMissionStatus"))
                .contains("QUIT");
    }
}
