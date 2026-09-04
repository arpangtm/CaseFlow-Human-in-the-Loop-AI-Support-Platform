package com.caseflow.support.recommendation;

import com.caseflow.support.knowledge.KnowledgeEvidence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

@Service
class RecommendationWriter {

    private final AiRecommendationRepository recommendationRepository;
    private final AiRecommendationCitationRepository citationRepository;
    private final RecommendationObservationRecorder observationRecorder;
    private final Clock clock;

    RecommendationWriter(
            AiRecommendationRepository recommendationRepository,
            AiRecommendationCitationRepository citationRepository,
            RecommendationObservationRecorder observationRecorder,
            Clock clock
    ) {
        this.recommendationRepository = recommendationRepository;
        this.citationRepository = citationRepository;
        this.observationRecorder = observationRecorder;
        this.clock = clock;
    }

    @Transactional
    RecommendationResponse persist(
            UUID organizationId,
            UUID caseId,
            ProviderRecommendation providerRecommendation,
            List<KnowledgeEvidence> retrievedEvidence,
            String promptVersion,
            String schemaVersion
    ) {
        int version = recommendationRepository.findMaxVersion(organizationId, caseId) + 1;
        AiRecommendation recommendation = new AiRecommendation(
                organizationId,
                caseId,
                version,
                providerRecommendation,
                promptVersion,
                schemaVersion,
                clock.instant()
        );
        AiRecommendation saved = recommendationRepository.saveAndFlush(recommendation);

        Map<UUID, KnowledgeEvidence> evidenceByChunkId = retrievedEvidence.stream()
                .collect(java.util.stream.Collectors.toMap(KnowledgeEvidence::chunkId, Function.identity()));
        List<AiRecommendationCitation> citations = IntStream.range(
                        0,
                        providerRecommendation.citationChunkIds().size()
                )
                .mapToObj(position -> new AiRecommendationCitation(
                        saved.getId(),
                        organizationId,
                        position,
                        evidenceByChunkId.get(providerRecommendation.citationChunkIds().get(position))
                ))
                .toList();
        citationRepository.saveAll(citations);
        ProviderMetadata metadata = providerRecommendation.metadata();
        observationRecorder.record(new RecommendationObservation(
                organizationId,
                caseId,
                saved.getId(),
                metadata.provider(),
                metadata.model(),
                metadata.latencyMs(),
                metadata.inputTokens(),
                metadata.outputTokens(),
                saved.getCreatedAt()
        ));
        return RecommendationResponse.from(saved, citations);
    }

    @Transactional(readOnly = true)
    List<RecommendationResponse> list(UUID organizationId, UUID caseId) {
        return recommendationRepository
                .findAllByOrganizationIdAndCaseIdOrderByRecommendationVersionDesc(organizationId, caseId)
                .stream()
                .map(recommendation -> RecommendationResponse.from(
                        recommendation,
                        citationRepository.findAllByOrganizationIdAndRecommendationIdOrderByPositionAsc(
                                organizationId,
                                recommendation.getId()
                        )
                ))
                .toList();
    }
}
