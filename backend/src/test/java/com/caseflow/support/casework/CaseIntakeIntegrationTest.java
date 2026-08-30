package com.caseflow.support.casework;

import com.caseflow.support.CaseFlowApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = CaseFlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class CaseIntakeIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Test
    void createsAndListsAnAutomaticallyClassifiedCase() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String body = """
                {
                  "organizationId": "00000000-0000-0000-0000-000000000001",
                  "customerId": "00000000-0000-0000-0000-000000000002",
                  "subject": "Production down",
                  "description": "There is a service outage for all users"
                }
                """;

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/cases"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(createResponse.statusCode()).isEqualTo(201);
        assertThat(createResponse.body()).contains(
                "\"category\":\"TECHNICAL\"",
                "\"priority\":\"CRITICAL\"",
                "\"status\":\"NEW\""
        );

        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/v1/cases?organizationId=00000000-0000-0000-0000-000000000001"))
                .GET()
                .build();
        HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body()).contains("Production down", "\"priority\":\"CRITICAL\"");
    }
}
