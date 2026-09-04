package com.caseflow.support.observability;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class EvaluationService {

    private final RecommendationEvaluationRepository repository;

    EvaluationService(RecommendationEvaluationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    EvaluationResponse get(UUID recommendationId, UUID organizationId) {
        return repository.findByOrganizationIdAndRecommendationId(organizationId, recommendationId)
                .map(EvaluationResponse::from)
                .orElseThrow(() -> new EvaluationNotFoundException(recommendationId));
    }
}
