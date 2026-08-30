package com.caseflow.support.casework;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaseClassifierTest {

    private final CaseClassifier classifier = new CaseClassifier();

    @Test
    void classifiesAProductionOutageAsCriticalTechnical() {
        CaseClassification result = classifier.classify(
                "Production down",
                "There is a service outage for every region"
        );

        assertThat(result.category()).isEqualTo(CaseCategory.TECHNICAL);
        assertThat(result.priority()).isEqualTo(CasePriority.CRITICAL);
    }

    @Test
    void classifiesLoginFailureAsHighPriorityAccess() {
        CaseClassification result = classifier.classify(
                "Can't login",
                "My account is locked"
        );

        assertThat(result.category()).isEqualTo(CaseCategory.ACCESS);
        assertThat(result.priority()).isEqualTo(CasePriority.HIGH);
    }

    @Test
    void classifiesBillingQuestionAsLowPriorityBilling() {
        CaseClassification result = classifier.classify(
                "Invoice question",
                "How do I download last month's invoice?"
        );

        assertThat(result.category()).isEqualTo(CaseCategory.BILLING);
        assertThat(result.priority()).isEqualTo(CasePriority.LOW);
    }
}

