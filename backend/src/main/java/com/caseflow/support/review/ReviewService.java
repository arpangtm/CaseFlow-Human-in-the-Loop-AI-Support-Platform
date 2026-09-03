package com.caseflow.support.review;

import com.caseflow.support.recommendation.RecommendationAlreadyReviewedException;
import com.caseflow.support.recommendation.RecommendationReviewGateway;
import com.caseflow.support.recommendation.ReviewableRecommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
class ReviewService {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";

    private final RecommendationReviewGateway recommendationGateway;
    private final SupportUserRepository userRepository;
    private final ReviewDecisionRepository decisionRepository;
    private final ReviewDecisionValidator validator;
    private final Clock clock;

    ReviewService(
            RecommendationReviewGateway recommendationGateway,
            SupportUserRepository userRepository,
            ReviewDecisionRepository decisionRepository,
            ReviewDecisionValidator validator,
            Clock clock
    ) {
        this.recommendationGateway = recommendationGateway;
        this.userRepository = userRepository;
        this.decisionRepository = decisionRepository;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    ReviewDecisionResponse submit(
            UUID recommendationId,
            CreateReviewDecisionRequest request
    ) {
        ReviewableRecommendation recommendation = recommendationGateway.get(
                recommendationId,
                request.organizationId()
        );
        if (!PENDING_REVIEW.equals(recommendation.status())
                || decisionRepository.existsByOrganizationIdAndRecommendationId(
                request.organizationId(),
                recommendationId
        )) {
            throw new RecommendationAlreadyReviewedException(recommendationId);
        }

        SupportUser reviewer = userRepository
                .findByIdAndOrganizationId(request.reviewerId(), request.organizationId())
                .orElseThrow(() -> new ReviewerNotFoundException(request.reviewerId()));
        ReviewPayload payload = validator.validate(request, recommendation);
        Instant reviewedAt = clock.instant();
        long latencyMs = Math.max(0, Duration.between(recommendation.createdAt(), reviewedAt).toMillis());
        ReviewDecision decision = new ReviewDecision(
                request.organizationId(),
                recommendationId,
                recommendation.caseId(),
                request.reviewerId(),
                request.decision(),
                payload,
                latencyMs,
                reviewedAt
        );
        ReviewDecision saved = decisionRepository.saveAndFlush(decision);
        recommendationGateway.markReviewed(recommendationId, request.organizationId());
        return ReviewDecisionResponse.from(saved, reviewer);
    }

    @Transactional(readOnly = true)
    ReviewDecisionResponse get(UUID recommendationId, UUID organizationId) {
        recommendationGateway.get(recommendationId, organizationId);
        ReviewDecision review = decisionRepository
                .findByOrganizationIdAndRecommendationId(organizationId, recommendationId)
                .orElseThrow(() -> new ReviewDecisionNotFoundException(recommendationId));
        SupportUser reviewer = userRepository
                .findByIdAndOrganizationId(review.getReviewerId(), organizationId)
                .orElseThrow(() -> new ReviewerNotFoundException(review.getReviewerId()));
        return ReviewDecisionResponse.from(review, reviewer);
    }
}
