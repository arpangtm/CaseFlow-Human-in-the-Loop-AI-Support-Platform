package com.caseflow.support.casework;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_cases")
class SupportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 10_000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CaseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CasePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CaseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected SupportCase() {
    }

    SupportCase(UUID organizationId, UUID customerId, String subject, String description,
                CaseCategory category, CasePriority priority, Instant now) {
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.subject = subject;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = CaseStatus.NEW;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID getId() {
        return id;
    }

    UUID getOrganizationId() {
        return organizationId;
    }

    UUID getCustomerId() {
        return customerId;
    }

    String getSubject() {
        return subject;
    }

    String getDescription() {
        return description;
    }

    CaseCategory getCategory() {
        return category;
    }

    CasePriority getPriority() {
        return priority;
    }

    CaseStatus getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}

