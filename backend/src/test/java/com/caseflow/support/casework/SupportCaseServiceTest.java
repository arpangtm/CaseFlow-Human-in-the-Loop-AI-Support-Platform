package com.caseflow.support.casework;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupportCaseServiceTest {

    @Test
    void trimsAndPersistsANewClassifiedCase() {
        SupportCaseRepository repository = mock(SupportCaseRepository.class);
        when(repository.save(any(SupportCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T14:00:00Z"), ZoneOffset.UTC);
        SupportCaseService service = new SupportCaseService(repository, new CaseClassifier(), clock);

        CaseResponse response = service.create(new CreateCaseRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "  Refund needed  ",
                "  I was charged twice  "
        ));

        assertThat(response.subject()).isEqualTo("Refund needed");
        assertThat(response.description()).isEqualTo("I was charged twice");
        assertThat(response.category()).isEqualTo(CaseCategory.BILLING);
        assertThat(response.priority()).isEqualTo(CasePriority.NORMAL);
        assertThat(response.status()).isEqualTo(CaseStatus.NEW);
        assertThat(response.createdAt()).isEqualTo(clock.instant());
    }
}

