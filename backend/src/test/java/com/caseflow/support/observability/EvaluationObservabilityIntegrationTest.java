package com.caseflow.support.observability;

import com.caseflow.support.CaseFlowApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = CaseFlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class EvaluationObservabilityIntegrationTest {

    private static final String DEMO_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final String DEMO_CUSTOMER_ID = "00000000-0000-0000-0000-000000000002";
    private static final String DEMO_REVIEWER_ID = "00000000-0000-0000-0000-000000000003";
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
    void evaluatesAReviewedRecommendationAndPublishesSafeMetrics() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> articleResponse = postJson(client, "/api/v1/knowledge/articles", """
                {
                  "organizationId": "%s",
                  "title": "Reset a customer password",
                  "content": "Password reset links expire after fifteen minutes. Generate a new link from Account Settings."
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

        HttpRequest generateRequest = HttpRequest.newBuilder()
                .uri(apiUri("/api/v1/cases/" + caseId + "/recommendations?organizationId="
                        + DEMO_ORGANIZATION_ID))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> generationResponse = client.send(
                generateRequest,
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(generationResponse.statusCode()).isEqualTo(201);
        String recommendationId = extractId(generationResponse.body());

        HttpResponse<String> reviewResponse = postJson(
                client,
                "/api/v1/recommendations/" + recommendationId + "/review",
                """
                        {
                          "organizationId": "%s",
                          "reviewerId": "%s",
                          "decision": "EDITED",
                          "editedResponse": "Please generate a fresh reset link from Account Settings."
                        }
                        """.formatted(DEMO_ORGANIZATION_ID, DEMO_REVIEWER_ID)
        );
        assertThat(reviewResponse.statusCode()).isEqualTo(201);

        HttpResponse<String> evaluationResponse = get(
                client,
                "/api/v1/recommendations/" + recommendationId + "/evaluation?organizationId="
                        + DEMO_ORGANIZATION_ID
        );
        assertThat(evaluationResponse.statusCode()).isEqualTo(200);
        assertThat(evaluationResponse.body()).contains(
                "\"recommendationId\":\"" + recommendationId + "\"",
                "\"reviewOutcome\":\"EDITED\"",
                "\"schemaValid\":true",
                "\"citationsValid\":true",
                "\"citationCount\":1",
                "\"normalizedEditDistance\":"
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM agent_execution_logs WHERE recommendation_id = ?::uuid",
                Long.class,
                recommendationId
        )).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ai_evaluations WHERE recommendation_id = ?::uuid",
                Long.class,
                recommendationId
        )).isEqualTo(1L);

        HttpResponse<String> metricResponse = get(
                client,
                "/actuator/metrics/caseflow.ai.reviews?tag=decision:edited"
        );
        assertThat(metricResponse.statusCode()).isEqualTo(200);
        assertThat(metricResponse.body()).contains(
                "\"name\":\"caseflow.ai.reviews\"",
                "\"statistic\":\"COUNT\""
        );

        jdbcTemplate.update(
                "INSERT INTO organizations (id, name) VALUES (?::uuid, ?)",
                OTHER_ORGANIZATION_ID,
                "Other Organization"
        );
        HttpResponse<String> crossTenantResponse = get(
                client,
                "/api/v1/recommendations/" + recommendationId + "/evaluation?organizationId="
                        + OTHER_ORGANIZATION_ID
        );
        assertThat(crossTenantResponse.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> postJson(HttpClient client, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiUri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(apiUri(path)).GET().build();
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
}
