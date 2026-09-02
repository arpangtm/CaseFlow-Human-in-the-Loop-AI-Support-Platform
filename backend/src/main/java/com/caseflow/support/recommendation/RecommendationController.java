package com.caseflow.support.recommendation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/recommendations")
class RecommendationController {

    private final RecommendationService service;

    RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RecommendationResponse generate(
            @PathVariable UUID caseId,
            @RequestParam UUID organizationId
    ) {
        return service.generate(organizationId, caseId);
    }

    @GetMapping
    List<RecommendationResponse> list(
            @PathVariable UUID caseId,
            @RequestParam UUID organizationId
    ) {
        return service.list(organizationId, caseId);
    }
}
