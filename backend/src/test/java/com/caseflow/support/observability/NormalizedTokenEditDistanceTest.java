package com.caseflow.support.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizedTokenEditDistanceTest {

    private final NormalizedTokenEditDistance editDistance = new NormalizedTokenEditDistance();

    @Test
    void returnsZeroForEquivalentResponsesIgnoringCaseAndSpacing() {
        assertThat(editDistance.calculate("Use the RESET link", " use  the reset link ")).isZero();
    }

    @Test
    void normalizesTokenChangesByTheLongerResponse() {
        assertThat(editDistance.calculate("reset link", "new reset link"))
                .isCloseTo(1.0 / 3.0, within(0.00001));
    }

    @Test
    void handlesEmptyResponsesWithoutDividingByZero() {
        assertThat(editDistance.calculate("", "")).isZero();
        assertThat(editDistance.calculate("", "human response")).isEqualTo(1.0);
    }

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
