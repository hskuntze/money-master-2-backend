package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditReverseResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiCommandAuditResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandResult;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentReverseRequest;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAuditReversal;
import br.com.kuntzedevprojects.money_master_2.entities.Payment;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandReversalStatus;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditReversalRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentAnticipationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.finance.installment.InstallmentAnticipationService;
import br.com.kuntzedevprojects.money_master_2.services.finance.payment.PaymentService;

class AiCommandAuditServiceTest {

    private final AiCommandAuditRepository repository = mock(AiCommandAuditRepository.class);
    private final AiCommandAuditReversalRepository reversalRepository = mock(AiCommandAuditReversalRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final InstallmentAnticipationRepository anticipationRepository = mock(InstallmentAnticipationRepository.class);
    private final PaymentService paymentService = mock(PaymentService.class);
    private final InstallmentAnticipationService anticipationService = mock(InstallmentAnticipationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiCommandAuditService service = new AiCommandAuditService(
            repository,
            reversalRepository,
            objectMapper,
            paymentRepository,
            anticipationRepository,
            paymentService,
            anticipationService
    );

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
        when(reversalRepository.findByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(Optional.empty());
        when(reversalRepository.existsByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(false);
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
        when(reversalRepository.findByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(Optional.empty());
        when(reversalRepository.existsByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(false);
        when(paymentRepository.findByIdAndOwnerEmail(77L, "ana@example.com")).thenReturn(Optional.of(activePayment()));
        when(repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(eq("ana@example.com"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(audit));

        AiCommandAuditResponse response = service.listRecent("ana@example.com", 10).get(0);

        assertThat(response.reversal().status()).isEqualTo(AiCommandReversalStatus.DIRECTLY_SUPPORTED);
        assertThat(response.reversal().referenceType()).isEqualTo("PAYMENT");
        assertThat(response.reversal().referenceId()).isEqualTo(77L);
    }

    @Test
    void shouldMarkPaymentAuditAsNotApplicableWhenPaymentIsAlreadyInactive() throws Exception {
        AiCommandAudit audit = executedPaymentAudit();
        Payment payment = activePayment();
        payment.setStatus(PaymentStatus.REVERSED);
        when(reversalRepository.findByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(Optional.empty());
        when(reversalRepository.existsByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(false);
        when(paymentRepository.findByIdAndOwnerEmail(77L, "ana@example.com")).thenReturn(Optional.of(payment));
        when(repository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(eq("ana@example.com"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(audit));

        AiCommandAuditResponse response = service.listRecent("ana@example.com", 10).get(0);

        assertThat(response.reversal().status()).isEqualTo(AiCommandReversalStatus.NOT_APPLICABLE);
        assertThat(response.reversal().label()).contains("ja revertido");
    }

    @Test
    void shouldReverseDirectlySupportedPaymentAudit() throws Exception {
        AiCommandAudit audit = executedPaymentAudit();
        when(repository.findByIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(Optional.of(audit));
        when(reversalRepository.existsByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(false);
        when(reversalRepository.findByAuditIdAndOwnerEmailIgnoreCase(10L, "ana@example.com")).thenReturn(Optional.empty());
        when(reversalRepository.save(any(AiCommandAuditReversal.class))).thenAnswer(invocation -> {
            AiCommandAuditReversal reversal = invocation.getArgument(0);
            reversal.setId(12L);
            reversal.setCreatedAt(Instant.parse("2026-08-28T10:00:00Z"));
            return reversal;
        });
        when(paymentRepository.findByIdAndOwnerEmail(77L, "ana@example.com")).thenReturn(Optional.of(activePayment()));

        AiCommandAuditReverseResponse response = service.reverse("ana@example.com", 10L, null);

        ArgumentCaptor<PaymentReverseRequest> requestCaptor = ArgumentCaptor.forClass(PaymentReverseRequest.class);
        ArgumentCaptor<AiCommandAuditReversal> reversalCaptor = ArgumentCaptor.forClass(AiCommandAuditReversal.class);
        verify(paymentService).reverse(eq("ana@example.com"), eq(77L), requestCaptor.capture());
        verify(reversalRepository).save(reversalCaptor.capture());
        assertThat(requestCaptor.getValue().deleteLinkedTransaction()).isFalse();
        assertThat(requestCaptor.getValue().notes()).contains("auditoria da IA #10");
        assertThat(reversalCaptor.getValue().getAudit()).isSameAs(audit);
        assertThat(reversalCaptor.getValue().getReferenceType()).isEqualTo("PAYMENT");
        assertThat(response.referenceType()).isEqualTo("PAYMENT");
        assertThat(response.referenceId()).isEqualTo(77L);
        assertThat(response.reversalEvent().id()).isEqualTo(12L);
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

    private AiCommandAudit executedPaymentAudit() throws Exception {
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
        return audit;
    }

    private Payment activePayment() {
        Payment payment = new Payment();
        payment.setId(77L);
        payment.setStatus(PaymentStatus.ACTIVE);
        return payment;
    }
}
