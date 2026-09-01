package com.caseflow.support.knowledge;

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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = CaseFlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class KnowledgeRetrievalIntegrationTest {

    private static final String DEMO_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000001";
    private static final String OTHER_ORGANIZATION_ID = "00000000-0000-0000-0000-000000000099";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void ingestsAndRetrievesKnowledgeWithoutCrossingOrganizationBoundaries() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO organizations (id, name) VALUES (?::uuid, ?)",
                OTHER_ORGANIZATION_ID,
                "Other Organization"
        );
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> createResponse = createArticle(
                client,
                DEMO_ORGANIZATION_ID,
                "Reset a customer password",
                "Password reset links expire after fifteen minutes. Generate a new link from Account Settings."
        );
        createArticle(
                client,
                OTHER_ORGANIZATION_ID,
                "Internal password reset procedure",
                "Password reset requests for internal administrators require a security escalation."
        );

        assertThat(createResponse.statusCode()).isEqualTo(201);
        assertThat(createResponse.body()).contains(
                "\"status\":\"ACTIVE\"",
                "\"chunkCount\":1"
        );

        String query = URLEncoder.encode("password reset", StandardCharsets.UTF_8);
        HttpRequest searchRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/v1/knowledge/search?organizationId=" + DEMO_ORGANIZATION_ID
                        + "&query=" + query
                        + "&limit=5"))
                .GET()
                .build();
        HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(searchResponse.statusCode()).isEqualTo(200);
        assertThat(searchResponse.body()).contains(
                "\"query\":\"password reset\"",
                "Reset a customer password",
                "Password reset links expire after fifteen minutes",
                "\"score\":"
        );
        assertThat(searchResponse.body()).doesNotContain("Internal password reset procedure");
    }

    private HttpResponse<String> createArticle(
            HttpClient client,
            String organizationId,
            String title,
            String content
    ) throws Exception {
        String body = """
                {
                  "organizationId": "%s",
                  "title": "%s",
                  "content": "%s",
                  "sourceUrl": "https://docs.example.com/passwords"
                }
                """.formatted(organizationId, title, content);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/knowledge/articles"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
