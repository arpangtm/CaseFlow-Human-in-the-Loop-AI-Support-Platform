package com.caseflow.support.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {
}
