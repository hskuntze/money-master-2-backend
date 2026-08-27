package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandResult;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandReversalStatus;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;

class AiCommandAuditServiceTest {

    private final AiCommandAuditRepository repository = mock(AiCommandAuditRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiCommandAuditService service = new AiCommandAuditService(repository, objectMapper);

    @Test
    void shouldListRecentAuditsWithReadableResultSummary() throws Exception {
        AiCommandAudit audit = audit();
        FinanceCommandResult result = new FinanceCommandResult(
                FinanceCommandType.REGISTER_TRANSACTION,
                AiCommandStatus.PREVIEWED,
                "Vou registrar a despesa.",
                true,
                null
        );
        audit.setResultJson(objectMapper.writeValueAsString(result));
        when(repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(eq("ana@example.com"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(audit));

        List<AiCommandAuditResponse> response = service.listRecent("ana@example.com", 20);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).commandType()).isEqualTo(FinanceCommandType.REGISTER_TRANSACTION);
        assertThat(response.get(0).status()).isEqualTo(AiCommandStatus.PREVIEWED);
        assertThat(response.get(0).message()).isEqualTo("Vou registrar a despesa.");
        assertThat(response.get(0).requiresConfirmation()).isTrue();
        assertThat(response.get(0).reversal().status()).isEqualTo(AiCommandReversalStatus.NOT_APPLICABLE);
    }

    @Test
    void shouldClampRequestedLimit() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(eq("ana@example.com"), pageableCaptor.capture()))
                .thenReturn(List.of());

        service.listRecent("ana@example.com", 500);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void shouldExposeDirectPaymentReversalReferenceWhenAuditHasExecutedPaymentResult() throws Exception {
        AiCommandAudit audit = audit();
        audit.setCommandType(FinanceCommandType.REGISTER_PAYMENT);
        audit.setStatus(AiCommandStatus.EXECUTED);
        audit.setDryRun(false);
        FinanceCommandResult result = new FinanceCommandResult(
                FinanceCommandType.REGISTER_PAYMENT,
                AiCommandStatus.EXECUTED,
                "Pagamento registrado com sucesso.",
                false,
                Map.of("payment", Map.of("id", 77L))
        );
        audit.setResultJson(objectMapper.writeValueAsString(result));
        when(repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(eq("ana@example.com"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(audit));

        AiCommandAuditResponse response = service.listRecent("ana@example.com", 10).get(0);

        assertThat(response.reversal().status()).isEqualTo(AiCommandReversalStatus.DIRECTLY_SUPPORTED);
        assertThat(response.reversal().referenceType()).isEqualTo("PAYMENT");
        assertThat(response.reversal().referenceId()).isEqualTo(77L);
    }

    private AiCommandAudit audit() {
        AiCommandAudit audit = new AiCommandAudit();
        audit.setId(10L);
        audit.setCommandType(FinanceCommandType.REGISTER_TRANSACTION);
        audit.setStatus(AiCommandStatus.PREVIEWED);
        audit.setDryRun(true);
        audit.setCommandJson("{\"type\":\"REGISTER_TRANSACTION\"}");
        audit.setCreatedAt(Instant.parse("2026-08-27T12:00:00Z"));
        return audit;
    }
}
