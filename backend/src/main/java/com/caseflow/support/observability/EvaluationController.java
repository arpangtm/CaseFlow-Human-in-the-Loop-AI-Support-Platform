package com.caseflow.support.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations/{recommendationId}/evaluation")
class EvaluationController {

    private final EvaluationService service;

    EvaluationController(EvaluationService service) {
        this.service = service;
    }

    @GetMapping
    EvaluationResponse get(
            @PathVariable UUID recommendationId,
            @RequestParam UUID organizationId
    ) {
        return service.get(recommendationId, organizationId);
    }
}
