package com.caseflow.support.casework;

import java.util.UUID;

public interface CaseReader {

    CaseResponse get(UUID id, UUID organizationId);
}
