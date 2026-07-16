package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandItem;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoicePaymentService;
import br.com.kuntzedevprojects.money_master_2.services.finance.installment.InstallmentAnticipationService;
import br.com.kuntzedevprojects.money_master_2.services.finance.payment.PaymentService;
import br.com.kuntzedevprojects.money_master_2.services.finance.savings.SavingsJarContributionPlanService;

class FinanceCommandExecutorConfirmationTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final SavingsJarService savingsJarService = mock(SavingsJarService.class);
    private final FinancialTransactionService transactionService = mock(FinancialTransactionService.class);
    private final FinancialReportService reportService = mock(FinancialReportService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final MonthlyPlanReconciliationService reconciliationService = mock(MonthlyPlanReconciliationService.class);
    private final InstallmentPurchaseService installmentPurchaseService = mock(InstallmentPurchaseService.class);
    private final PaymentService paymentService = mock(PaymentService.class);
    private final CreditCardInvoicePaymentService creditCardInvoicePaymentService = mock(CreditCardInvoicePaymentService.class);
    private final InstallmentAnticipationService installmentAnticipationService = mock(InstallmentAnticipationService.class);
    private final SavingsJarContributionPlanService savingsJarContributionPlanService = mock(SavingsJarContributionPlanService.class);
    private final FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
    private final AiCommandAuditRepository auditRepository = mock(AiCommandAuditRepository.class);
    private final AiCommandConfirmationService confirmationService = mock(AiCommandConfirmationService.class);
    private final AiPrivacySettingsService privacySettingsService = mock(AiPrivacySettingsService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final FinanceCommandExecutor executor = new FinanceCommandExecutor(
            currentUserService,
            accountService,
            categoryService,
            savingsJarService,
            transactionService,
            reportService,
            financialPeriodService,
            reconciliationService,
            installmentPurchaseService,
            paymentService,
            creditCardInvoicePaymentService,
            installmentAnticipationService,
            savingsJarContributionPlanService,
            transactionRepository,
            auditRepository,
            confirmationService,
            privacySettingsService,
            objectMapper
    );

    @Test
    void shouldBlockSensitiveExecutionWithoutValidConfirmationBeforeSideEffects() {
        User owner = owner();
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner);
        when(auditRepository.save(any(AiCommandAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new BusinessException("token obrigatorio"))
                .when(confirmationService)
                .validateAndConsume(anyString(), anyList(), any());

        FinanceCommandBatchResponse response = executor.execute("ana@example.com", new FinanceCommandBatchRequest(
                false,
                "registrar gasto",
                null,
                List.of(command())
        ));

        assertThat(response.executed()).isFalse();
        assertThat(response.requiresConfirmation()).isTrue();
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo(AiCommandStatus.SKIPPED);
        verifyNoInteractions(transactionService);
    }

    private FinanceCommandItem command() {
        try {
            return objectMapper.readValue("""
                    {
                      "type": "REGISTER_TRANSACTION",
                      "transactionType": "EXPENSE",
                      "amount": 45.90,
                      "description": "Mercado",
                      "occurredOn": "2026-07-16",
                      "categoryName": "Alimentacao"
                    }
                    """, FinanceCommandItem.class);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private User owner() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana");
        user.setEmail("ana@example.com");
        return user;
    }
}
