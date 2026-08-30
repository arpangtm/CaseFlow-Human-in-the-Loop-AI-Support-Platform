package com.caseflow.support.casework;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
class CaseClassifier {

    private static final Set<String> CRITICAL_SIGNALS = Set.of(
            "security breach", "data loss", "all users down", "service outage", "production down"
    );
    private static final Set<String> HIGH_SIGNALS = Set.of(
            "can't login", "cannot login", "payment failed", "blocked", "unable to", "urgent"
    );

    CaseClassification classify(String subject, String description) {
        String text = (subject + " " + description).toLowerCase(Locale.ROOT);
        CaseCategory category = categoryFor(text);
        CasePriority priority = priorityFor(text);
        return new CaseClassification(category, priority);
    }

    private CaseCategory categoryFor(String text) {
        if (containsAny(text, "login", "password", "sign in", "account locked", "access")) {
            return CaseCategory.ACCESS;
        }
        if (containsAny(text, "invoice", "billing", "payment", "charge", "refund", "subscription")) {
            return CaseCategory.BILLING;
        }
        if (containsAny(text, "error", "bug", "outage", "down", "crash", "api", "integration")) {
            return CaseCategory.TECHNICAL;
        }
        return CaseCategory.GENERAL;
    }

    private CasePriority priorityFor(String text) {
        if (CRITICAL_SIGNALS.stream().anyMatch(text::contains)) {
            return CasePriority.CRITICAL;
        }
        if (HIGH_SIGNALS.stream().anyMatch(text::contains)) {
            return CasePriority.HIGH;
        }
        if (containsAny(text, "question", "how do i", "feature request")) {
            return CasePriority.LOW;
        }
        return CasePriority.NORMAL;
    }

    private boolean containsAny(String text, String... signals) {
        for (String signal : signals) {
            if (text.contains(signal)) {
                return true;
            }
        }
        return false;
    }
}

