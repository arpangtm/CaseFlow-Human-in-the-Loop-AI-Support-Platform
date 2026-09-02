package com.caseflow.support.recommendation;

import com.caseflow.support.casework.CaseResponse;
import com.caseflow.support.knowledge.KnowledgeEvidence;

import java.util.List;

public record RecommendationRequest(
        CaseResponse supportCase,
        List<KnowledgeEvidence> evidence,
        String promptVersion,
        String schemaVersion
) {
    public RecommendationRequest {
        evidence = List.copyOf(evidence);
    }
}
