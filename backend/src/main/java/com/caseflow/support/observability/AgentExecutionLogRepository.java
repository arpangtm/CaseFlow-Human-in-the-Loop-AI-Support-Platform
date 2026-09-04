package com.caseflow.support.observability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AgentExecutionLogRepository extends JpaRepository<AgentExecutionLog, UUID> {
}
