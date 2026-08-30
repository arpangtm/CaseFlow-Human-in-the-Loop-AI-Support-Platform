package com.caseflow.support.casework;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCaseRequest(
        @NotNull UUID organizationId,
        @NotNull UUID customerId,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 10_000) String description
) {
}

