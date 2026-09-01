package com.caseflow.support.knowledge;

import java.util.UUID;

interface KnowledgeSearchRow {

    UUID getArticleId();

    UUID getChunkId();

    String getTitle();

    String getContent();

    String getSourceUrl();

    double getScore();
}
