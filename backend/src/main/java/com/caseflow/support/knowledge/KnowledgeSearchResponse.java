package com.caseflow.support.knowledge;

import java.util.List;
import java.util.UUID;

record KnowledgeSearchResponse(
        UUID organizationId,
        String query,
        List<KnowledgeEvidence> evidence
) {
}
