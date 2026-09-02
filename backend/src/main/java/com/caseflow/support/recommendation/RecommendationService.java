package com.caseflow.support.recommendation;

import com.caseflow.support.casework.CaseReader;
import com.caseflow.support.casework.CaseResponse;
import com.caseflow.support.knowledge.KnowledgeEvidence;
import com.caseflow.support.knowledge.KnowledgeRetriever;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class RecommendationService {

    static final String PROMPT_VERSION = "case-recommendation-v1";
    static final String SCHEMA_VERSION = "recommendation-output-v1";
    private static final int EVIDENCE_LIMIT = 5;

    private final CaseReader caseReader;
    private final KnowledgeRetriever knowledgeRetriever;
    private final RecommendationProvider provider;
    private final RecommendationValidator validator;
    private final RecommendationWriter writer;

    RecommendationService(
            CaseReader caseReader,
            KnowledgeRetriever knowledgeRetriever,
            RecommendationProvider provider,
            RecommendationValidator validator,
            RecommendationWriter writer
    ) {
        this.caseReader = caseReader;
        this.knowledgeRetriever = knowledgeRetriever;
        this.provider = provider;
        this.validator = validator;
        this.writer = writer;
    }

    RecommendationResponse generate(UUID organizationId, UUID caseId) {
        CaseResponse supportCase = caseReader.get(caseId, organizationId);
        String retrievalQuery = buildRetrievalQuery(supportCase);
        List<KnowledgeEvidence> evidence = knowledgeRetriever
                .search(organizationId, retrievalQuery, EVIDENCE_LIMIT)
                .evidence();
        RecommendationRequest request = new RecommendationRequest(
                supportCase,
                evidence,
                PROMPT_VERSION,
                SCHEMA_VERSION
        );
        ProviderRecommendation recommendation = provider.generate(request);
        validator.validate(recommendation, evidence);
        return writer.persist(
                organizationId,
                caseId,
                recommendation,
                evidence,
                PROMPT_VERSION,
                SCHEMA_VERSION
        );
    }

    List<RecommendationResponse> list(UUID organizationId, UUID caseId) {
        caseReader.get(caseId, organizationId);
        return writer.list(organizationId, caseId);
    }

    private String buildRetrievalQuery(CaseResponse supportCase) {
        return supportCase.subject().strip();
    }
}
