package com.caseflow.support.casework;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
class SupportCaseController {

    private final SupportCaseService service;

    SupportCaseController(SupportCaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CaseResponse create(@Valid @RequestBody CreateCaseRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    CaseResponse get(@PathVariable UUID id, @RequestParam UUID organizationId) {
        return service.get(id, organizationId);
    }

    @GetMapping
    List<CaseResponse> list(@RequestParam UUID organizationId) {
        return service.list(organizationId);
    }
}
