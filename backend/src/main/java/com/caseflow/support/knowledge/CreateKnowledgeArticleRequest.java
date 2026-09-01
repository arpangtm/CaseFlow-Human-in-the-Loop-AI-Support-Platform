package com.caseflow.support.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record CreateKnowledgeArticleRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100_000) String content,
        @Size(max = 2048) String sourceUrl
) {
}
