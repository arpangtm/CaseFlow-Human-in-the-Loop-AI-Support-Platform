package com.caseflow.support.casework;

import java.time.Instant;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        UUID organizationId,
        UUID customerId,
        String subject,
        String description,
        CaseCategory category,
        CasePriority priority,
        CaseStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    static CaseResponse from(SupportCase supportCase) {
        return new CaseResponse(
                supportCase.getId(),
                supportCase.getOrganizationId(),
                supportCase.getCustomerId(),
                supportCase.getSubject(),
                supportCase.getDescription(),
                supportCase.getCategory(),
                supportCase.getPriority(),
                supportCase.getStatus(),
                supportCase.getCreatedAt(),
                supportCase.getUpdatedAt()
        );
    }
}

