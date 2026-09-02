package com.caseflow.support.recommendation;

import com.caseflow.support.casework.CaseCategory;
import com.caseflow.support.casework.CasePriority;
import com.caseflow.support.casework.CaseReader;
import com.caseflow.support.casework.CaseResponse;
import com.caseflow.support.casework.CaseStatus;
import com.caseflow.support.knowledge.KnowledgeEvidence;
import com.caseflow.support.knowledge.KnowledgeRetriever;
import com.caseflow.support.knowledge.KnowledgeSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private CaseReader caseReader;

    @Mock
    private KnowledgeRetriever knowledgeRetriever;

    @Mock
    private RecommendationProvider provider;

    @Mock
    private RecommendationWriter writer;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(
                caseReader,
                knowledgeRetriever,
                provider,
                new RecommendationValidator(),
                writer
        );
    }

    @Test
    void retrievesEvidenceAndPersistsAValidatedStructuredRecommendation() {
        UUID organizationId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        CaseResponse supportCase = supportCase(organizationId, caseId);
        KnowledgeEvidence evidence = evidence(UUID.randomUUID());
        ProviderRecommendation generated = validRecommendation(evidence.chunkId());
        RecommendationResponse persisted = persistedRecommendation(organizationId, caseId);
        when(caseReader.get(caseId, organizationId)).thenReturn(supportCase);
        when(knowledgeRetriever.search(organizationId, "Password reset", 5))
                .thenReturn(new KnowledgeSearchResponse(organizationId, "Password reset", List.of(evidence)));
        when(provider.generate(any(RecommendationRequest.class))).thenReturn(generated);
        when(writer.persist(
                organizationId,
                caseId,
                generated,
                List.of(evidence),
                RecommendationService.PROMPT_VERSION,
                RecommendationService.SCHEMA_VERSION
        )).thenReturn(persisted);

        RecommendationResponse result = service.generate(organizationId, caseId);

        assertThat(result).isSameAs(persisted);
        verify(caseReader).get(caseId, organizationId);
        verify(knowledgeRetriever).search(organizationId, "Password reset", 5);
        verify(writer).persist(
                organizationId,
                caseId,
                generated,
                List.of(evidence),
                RecommendationService.PROMPT_VERSION,
                RecommendationService.SCHEMA_VERSION
        );
    }

    @Test
    void neverPersistsAProviderResultWithAnInventedCitation() {
        UUID organizationId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        KnowledgeEvidence evidence = evidence(UUID.randomUUID());
        when(caseReader.get(caseId, organizationId)).thenReturn(supportCase(organizationId, caseId));
        when(knowledgeRetriever.search(eq(organizationId), eq("Password reset"), eq(5)))
                .thenReturn(new KnowledgeSearchResponse(organizationId, "Password reset", List.of(evidence)));
        when(provider.generate(any(RecommendationRequest.class)))
                .thenReturn(validRecommendation(UUID.randomUUID()));

        assertThatThrownBy(() -> service.generate(organizationId, caseId))
                .isInstanceOf(RecommendationGenerationException.class)
                .hasMessageContaining("citations must reference retrieved evidence");
        verify(writer, never()).persist(any(), any(), any(), any(), any(), any());
    }

    private CaseResponse supportCase(UUID organizationId, UUID caseId) {
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        return new CaseResponse(
                caseId,
                organizationId,
                UUID.randomUUID(),
                "Password reset",
                "The customer cannot sign in.",
                CaseCategory.ACCESS,
                CasePriority.NORMAL,
                CaseStatus.NEW,
                now,
                now
        );
    }

    private KnowledgeEvidence evidence(UUID chunkId) {
        return new KnowledgeEvidence(
                UUID.randomUUID(),
                chunkId,
                "Reset a password",
                "Password reset links expire after fifteen minutes.",
                "https://docs.example.com/passwords",
                0.75
        );
    }

    private ProviderRecommendation validRecommendation(UUID chunkId) {
        return new ProviderRecommendation(
                "Use a new reset link.",
                RecommendedAction.RESPOND_WITH_GUIDANCE,
                0.8,
                false,
                List.of(chunkId),
                new ProviderMetadata("test", "test-model", Map.of("temperature", 0), 12, 30, 15)
        );
    }

    private RecommendationResponse persistedRecommendation(UUID organizationId, UUID caseId) {
        return new RecommendationResponse(
                UUID.randomUUID(),
                organizationId,
                caseId,
                1,
                "PENDING_REVIEW",
                "Use a new reset link.",
                "RESPOND_WITH_GUIDANCE",
                0.8,
                false,
                List.of(),
                new RecommendationModelResponse(
                        "test",
                        "test-model",
                        Map.of("temperature", 0),
                        RecommendationService.PROMPT_VERSION,
                        RecommendationService.SCHEMA_VERSION,
                        12,
                        30,
                        15
                ),
                Instant.parse("2026-09-02T12:00:00Z")
        );
    }
}
