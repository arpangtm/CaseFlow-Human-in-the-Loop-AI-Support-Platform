package com.caseflow.support.recommendation;

import com.caseflow.support.CaseFlowApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = CaseFlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(RecommendationIntegrationTest.TestProviderConfiguration.class)
class RecommendationIntegrationTest {

    private static final String DEMO_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final String DEMO_CUSTOMER_ID = "00000000-0000-0000-0000-000000000002";
    private static final String OTHER_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000099";
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generatesPersistsAndListsAReviewableRecommendation() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> articleResponse = postJson(client, "/api/v1/knowledge/articles", """
                {
                  "organizationId": "%s",
                  "title": "Reset a customer password",
                  "content": "Password reset links expire after fifteen minutes. Generate a new link from Account Settings.",
                  "sourceUrl": "https://docs.example.com/passwords"
                }
                """.formatted(DEMO_ORGANIZATION_ID));
        HttpResponse<String> caseResponse = postJson(client, "/api/v1/cases", """
                {
                  "organizationId": "%s",
                  "customerId": "%s",
                  "subject": "Password reset",
                  "description": "The customer cannot sign in after requesting a reset link."
                }
                """.formatted(DEMO_ORGANIZATION_ID, DEMO_CUSTOMER_ID));

        assertThat(articleResponse.statusCode()).isEqualTo(201);
        assertThat(caseResponse.statusCode()).isEqualTo(201);
        String caseId = extractId(caseResponse.body());

        HttpResponse<String> firstGeneration = generate(client, caseId, DEMO_ORGANIZATION_ID);
        HttpResponse<String> secondGeneration = generate(client, caseId, DEMO_ORGANIZATION_ID);

        assertThat(firstGeneration.statusCode()).isEqualTo(201);
        assertThat(firstGeneration.body()).contains(
                "\"version\":1",
                "\"status\":\"PENDING_REVIEW\"",
                "\"recommendedAction\":\"RESPOND_WITH_GUIDANCE\"",
                "\"citations\":[{",
                "Reset a customer password",
                "\"provider\":\"test\"",
                "\"model\":\"structured-fake-v1\"",
                "\"promptVersion\":\"case-recommendation-v1\"",
                "\"schemaVersion\":\"recommendation-output-v1\"",
                "\"inputTokens\":120",
                "\"outputTokens\":42"
        );
        assertThat(secondGeneration.statusCode()).isEqualTo(201);
        assertThat(secondGeneration.body()).contains("\"version\":2");

        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(apiUri("/api/v1/cases/" + caseId + "/recommendations?organizationId="
                        + DEMO_ORGANIZATION_ID))
                .GET()
                .build();
        HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body()).startsWith("[{\"id\":").contains("\"version\":2", "\"version\":1");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_recommendations WHERE case_id = ?::uuid AND status = 'PENDING_REVIEW'",
                Long.class,
                caseId
        )).isEqualTo(2L);

        jdbcTemplate.update(
                "INSERT INTO organizations (id, name) VALUES (?::uuid, ?)",
                OTHER_ORGANIZATION_ID,
                "Other Organization"
        );
        HttpResponse<String> crossTenantGeneration = generate(client, caseId, OTHER_ORGANIZATION_ID);
        assertThat(crossTenantGeneration.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> postJson(HttpClient client, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiUri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> generate(
            HttpClient client,
            String caseId,
            String organizationId
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiUri("/api/v1/cases/" + caseId + "/recommendations?organizationId=" + organizationId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI apiUri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String extractId(String responseBody) {
        Matcher matcher = ID_PATTERN.matcher(responseBody);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestProviderConfiguration {

        @Bean
        @Primary
        RecommendationProvider testRecommendationProvider() {
            return request -> new ProviderRecommendation(
                    "Use a fresh password reset link from Account Settings.",
                    RecommendedAction.RESPOND_WITH_GUIDANCE,
                    0.91,
                    false,
                    List.of(request.evidence().getFirst().chunkId()),
                    new ProviderMetadata(
                            "test",
                            "structured-fake-v1",
                            Map.of("temperature", 0, "responseFormat", "json_schema"),
                            18,
                            120,
                            42
                    )
            );
        }
    }
}
