package com.caseflow.support.casework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class SupportCaseService {

    private final SupportCaseRepository repository;
    private final CaseClassifier classifier;
    private final Clock clock;

    SupportCaseService(SupportCaseRepository repository, CaseClassifier classifier, Clock clock) {
        this.repository = repository;
        this.classifier = classifier;
        this.clock = clock;
    }

    @Transactional
    CaseResponse create(CreateCaseRequest request) {
        CaseClassification classification = classifier.classify(request.subject(), request.description());
        Instant now = clock.instant();
        SupportCase supportCase = new SupportCase(
                request.organizationId(),
                request.customerId(),
                request.subject().trim(),
                request.description().trim(),
                classification.category(),
                classification.priority(),
                now
        );
        return CaseResponse.from(repository.save(supportCase));
    }

    @Transactional(readOnly = true)
    CaseResponse get(UUID id, UUID organizationId) {
        return repository.findByIdAndOrganizationId(id, organizationId)
                .map(CaseResponse::from)
                .orElseThrow(() -> new CaseNotFoundException(id));
    }

    @Transactional(readOnly = true)
    List<CaseResponse> list(UUID organizationId) {
        return repository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(CaseResponse::from)
                .toList();
    }
}
