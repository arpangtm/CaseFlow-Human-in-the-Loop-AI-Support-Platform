package com.caseflow.support.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
class KnowledgeService implements KnowledgeRetriever {

    static final int MAX_RESULTS = 20;

    private final KnowledgeArticleRepository articleRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeChunker chunker;
    private final Clock clock;

    KnowledgeService(
            KnowledgeArticleRepository articleRepository,
            KnowledgeChunkRepository chunkRepository,
            KnowledgeChunker chunker,
            Clock clock
    ) {
        this.articleRepository = articleRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.clock = clock;
    }

    @Transactional
    KnowledgeArticleResponse create(CreateKnowledgeArticleRequest request) {
        Instant now = clock.instant();
        KnowledgeArticle article = new KnowledgeArticle(
                request.organizationId(),
                request.title(),
                request.content(),
                request.sourceUrl(),
                now
        );
        KnowledgeArticle saved = articleRepository.saveAndFlush(article);
        List<String> contentChunks = chunker.chunk(request.content());
        List<KnowledgeChunk> chunks = IntStream.range(0, contentChunks.size())
                .mapToObj(index -> new KnowledgeChunk(
                        request.organizationId(),
                        saved.getId(),
                        index,
                        contentChunks.get(index),
                        now
                ))
                .toList();
        chunkRepository.saveAll(chunks);
        return KnowledgeArticleResponse.from(saved, chunks.size());
    }

    @Transactional(readOnly = true)
    public KnowledgeSearchResponse search(UUID organizationId, String query, int limit) {
        String normalizedQuery = query.strip();
        int boundedLimit = Math.max(1, Math.min(limit, MAX_RESULTS));
        List<KnowledgeEvidence> evidence = chunkRepository
                .search(organizationId, normalizedQuery, boundedLimit)
                .stream()
                .map(KnowledgeEvidence::from)
                .toList();
        return new KnowledgeSearchResponse(organizationId, normalizedQuery, evidence);
    }
}
