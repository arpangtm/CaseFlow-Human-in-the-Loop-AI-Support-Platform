package com.caseflow.support.knowledge;

import java.util.UUID;

public interface KnowledgeRetriever {

    KnowledgeSearchResponse search(UUID organizationId, String query, int limit);
}
