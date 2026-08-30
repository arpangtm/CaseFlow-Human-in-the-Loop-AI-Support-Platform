package com.caseflow.support.casework;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SupportCaseRepository extends JpaRepository<SupportCase, UUID> {

    Optional<SupportCase> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<SupportCase> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
