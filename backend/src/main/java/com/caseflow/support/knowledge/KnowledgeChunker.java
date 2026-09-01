package com.caseflow.support.knowledge;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class KnowledgeChunker {

    static final int MAX_CHARS = 1200;
    static final int OVERLAP_CHARS = 160;
    private static final int MIN_BREAK_CHARS = 600;

    List<String> chunk(String content) {
        String normalized = content.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + MAX_CHARS, normalized.length());
            if (end < normalized.length()) {
                end = findNaturalBreak(normalized, start, end);
            }

            String chunk = normalized.substring(start, end).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end == normalized.length()) {
                break;
            }

            start = findOverlapStart(normalized, start, end);
        }
        return List.copyOf(chunks);
    }

    private int findNaturalBreak(String content, int start, int preferredEnd) {
        int minimumBreak = Math.min(start + MIN_BREAK_CHARS, preferredEnd);
        int paragraphBreak = content.lastIndexOf("\n\n", preferredEnd);
        if (paragraphBreak >= minimumBreak) {
            return paragraphBreak;
        }

        int wordBreak = content.lastIndexOf(' ', preferredEnd);
        return wordBreak >= minimumBreak ? wordBreak : preferredEnd;
    }

    private int findOverlapStart(String content, int previousStart, int end) {
        int target = Math.max(previousStart + 1, end - OVERLAP_CHARS);
        int wordBreak = content.indexOf(' ', target);
        if (wordBreak >= target && wordBreak < end) {
            return wordBreak + 1;
        }
        return target;
    }
}
