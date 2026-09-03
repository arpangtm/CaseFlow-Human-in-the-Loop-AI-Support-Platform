package com.caseflow.support.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SupportUserRepository extends JpaRepository<SupportUser, UUID> {

    Optional<SupportUser> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
