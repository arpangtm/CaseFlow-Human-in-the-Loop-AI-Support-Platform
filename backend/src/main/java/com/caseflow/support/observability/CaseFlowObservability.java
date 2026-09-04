package com.caseflow.support.observability;

import com.caseflow.support.recommendation.RecommendationObservation;
import com.caseflow.support.recommendation.RecommendationObservationRecorder;
import com.caseflow.support.review.HumanReviewDecision;
import com.caseflow.support.review.ReviewEvaluationEvent;
import com.caseflow.support.review.ReviewEvaluationRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
class CaseFlowObservability implements RecommendationObservationRecorder, ReviewEvaluationRecorder {

    private final AgentExecutionLogRepository executionLogRepository;
    private final RecommendationEvaluationRepository evaluationRepository;
    private final NormalizedTokenEditDistance editDistance;
    private final MeterRegistry meterRegistry;

    CaseFlowObservability(
            AgentExecutionLogRepository executionLogRepository,
            RecommendationEvaluationRepository evaluationRepository,
            NormalizedTokenEditDistance editDistance,
            MeterRegistry meterRegistry
    ) {
        this.executionLogRepository = executionLogRepository;
        this.evaluationRepository = evaluationRepository;
        this.editDistance = editDistance;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(RecommendationObservation observation) {
        executionLogRepository.save(new AgentExecutionLog(observation));
        meterRegistry.counter(
                "caseflow.ai.recommendations",
                "provider", observation.provider(),
                "outcome", "succeeded"
        ).increment();
        meterRegistry.timer(
                "caseflow.ai.recommendation.latency",
                "provider", observation.provider()
        ).record(Duration.ofMillis(observation.latencyMs()));
        recordTokens("input", observation.inputTokens(), observation.provider());
        recordTokens("output", observation.outputTokens(), observation.provider());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(ReviewEvaluationEvent event) {
        ReviewOutcome outcome = outcome(event.decision());
        Double distance = switch (event.decision()) {
            case APPROVED -> 0.0;
            case EDITED -> editDistance.calculate(event.draftResponse(), event.finalResponse());
            case REJECTED -> null;
        };
        evaluationRepository.save(new RecommendationEvaluation(event, outcome, distance));

        meterRegistry.counter(
                "caseflow.ai.reviews",
                "decision", event.decision().name().toLowerCase()
        ).increment();
        meterRegistry.timer("caseflow.ai.review.latency")
                .record(Duration.ofMillis(event.reviewLatencyMs()));
        if (distance != null) {
            meterRegistry.summary("caseflow.ai.review.edit.distance").record(distance);
        }
    }

    private ReviewOutcome outcome(HumanReviewDecision decision) {
        return switch (decision) {
            case APPROVED -> ReviewOutcome.ACCEPTED;
            case EDITED -> ReviewOutcome.EDITED;
            case REJECTED -> ReviewOutcome.REJECTED;
        };
    }

    private void recordTokens(String direction, Integer tokens, String provider) {
        if (tokens != null) {
            meterRegistry.summary(
                    "caseflow.ai.recommendation.tokens",
                    "direction", direction,
                    "provider", provider
            ).record(tokens);
        }
    }
}
