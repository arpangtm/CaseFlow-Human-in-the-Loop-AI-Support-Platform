package com.caseflow.support.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/knowledge")
class KnowledgeController {

    private final KnowledgeService service;

    KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    KnowledgeArticleResponse create(@Valid @RequestBody CreateKnowledgeArticleRequest request) {
        return service.create(request);
    }

    @GetMapping("/search")
    KnowledgeSearchResponse search(
            @RequestParam UUID organizationId,
            @RequestParam @NotBlank @Size(max = 500) String query,
            @RequestParam(defaultValue = "5") @Min(1) @Max(KnowledgeService.MAX_RESULTS) int limit
    ) {
        return service.search(organizationId, query, limit);
    }
}
