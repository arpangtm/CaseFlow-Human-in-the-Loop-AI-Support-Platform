package com.caseflow.support.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    @Query(value = """
            SELECT a.id AS "articleId",
                   c.id AS "chunkId",
                   a.title AS title,
                   c.content AS content,
                   a.source_url AS "sourceUrl",
                   CAST(ts_rank_cd(c.search_vector, websearch_to_tsquery('english', :query)) AS double precision) AS score
              FROM knowledge_chunks c
              JOIN knowledge_articles a
                ON a.id = c.article_id
               AND a.organization_id = c.organization_id
             WHERE c.organization_id = :organizationId
               AND a.status = 'ACTIVE'
               AND c.search_vector @@ websearch_to_tsquery('english', :query)
             ORDER BY score DESC, a.id, c.sequence_number
             LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeSearchRow> search(
            @Param("organizationId") UUID organizationId,
            @Param("query") String query,
            @Param("limit") int limit
    );
}
