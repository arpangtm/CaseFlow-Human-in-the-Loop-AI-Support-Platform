package com.caseflow.support.casework;

import java.util.UUID;

public class CaseNotFoundException extends RuntimeException {

    CaseNotFoundException(UUID id) {
        super("Support case not found: " + id);
    }
}
