package com.caseflow.support.observability;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
class NormalizedTokenEditDistance {

    double calculate(String original, String edited) {
        String[] originalTokens = tokenize(original);
        String[] editedTokens = tokenize(edited);
        int longest = Math.max(originalTokens.length, editedTokens.length);
        if (longest == 0) {
            return 0;
        }

        int[] previous = new int[editedTokens.length + 1];
        int[] current = new int[editedTokens.length + 1];
        for (int index = 0; index <= editedTokens.length; index++) {
            previous[index] = index;
        }

        for (int originalIndex = 1; originalIndex <= originalTokens.length; originalIndex++) {
            current[0] = originalIndex;
            for (int editedIndex = 1; editedIndex <= editedTokens.length; editedIndex++) {
                int substitutionCost = originalTokens[originalIndex - 1]
                        .equals(editedTokens[editedIndex - 1]) ? 0 : 1;
                current[editedIndex] = Math.min(
                        Math.min(current[editedIndex - 1] + 1, previous[editedIndex] + 1),
                        previous[editedIndex - 1] + substitutionCost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return (double) previous[editedTokens.length] / longest;
    }

    private String[] tokenize(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
    }
}
