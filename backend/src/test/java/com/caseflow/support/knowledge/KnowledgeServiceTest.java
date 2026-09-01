package com.caseflow.support.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeArticleRepository articleRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private KnowledgeChunker chunker;

    @Mock
    private Clock clock;

    @Mock
    private KnowledgeSearchRow row;

    @InjectMocks
    private KnowledgeService service;

    @Test
    void scopesSearchAndBoundsTheRequestedEvidenceCount() {
        UUID organizationId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        when(chunkRepository.search(organizationId, "password reset", KnowledgeService.MAX_RESULTS))
                .thenReturn(List.of(row));
        when(row.getArticleId()).thenReturn(articleId);
        when(row.getChunkId()).thenReturn(chunkId);
        when(row.getTitle()).thenReturn("Reset a password");
        when(row.getContent()).thenReturn("Reset links expire after fifteen minutes.");
        when(row.getSourceUrl()).thenReturn("https://docs.example.com/passwords");
        when(row.getScore()).thenReturn(0.75);

        KnowledgeSearchResponse response = service.search(organizationId, "  password reset  ", 200);

        assertThat(response.organizationId()).isEqualTo(organizationId);
        assertThat(response.query()).isEqualTo("password reset");
        assertThat(response.evidence()).singleElement().satisfies(evidence -> {
            assertThat(evidence.articleId()).isEqualTo(articleId);
            assertThat(evidence.chunkId()).isEqualTo(chunkId);
            assertThat(evidence.score()).isEqualTo(0.75);
        });
        verify(chunkRepository).search(organizationId, "password reset", KnowledgeService.MAX_RESULTS);
    }
}
