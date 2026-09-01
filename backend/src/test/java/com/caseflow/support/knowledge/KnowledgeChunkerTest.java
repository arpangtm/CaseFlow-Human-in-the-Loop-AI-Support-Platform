package com.caseflow.support.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkerTest {

    private final KnowledgeChunker chunker = new KnowledgeChunker();

    @Test
    void returnsNoChunksForBlankContent() {
        assertThat(chunker.chunk(" \n\t ")).isEmpty();
    }

    @Test
    void createsBoundedOverlappingChunksWithoutLosingOrder() {
        String content = "Password reset tokens expire after fifteen minutes. "
                .repeat(60)
                .strip();

        List<String> chunks = chunker.chunk(content);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk).isNotBlank();
            assertThat(chunk.length()).isLessThanOrEqualTo(KnowledgeChunker.MAX_CHARS);
        });
        assertThat(chunks.get(0)).contains(chunks.get(1).substring(0, 80));
    }

    @Test
    void keepsShortContentAsOneChunk() {
        assertThat(chunker.chunk("  Reset the password from Account Settings.  "))
                .containsExactly("Reset the password from Account Settings.");
    }
}
