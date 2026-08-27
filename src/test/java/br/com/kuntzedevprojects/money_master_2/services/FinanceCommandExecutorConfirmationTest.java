package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandBatchResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.FinanceCommandItem;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementResponse;
import br.com.kuntzedevprojects.money_master_2.entities.InvestmentProduct;
import br.com.kuntzedevprojects.money_master_2.entities.AiCommandAudit;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AiCommandStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InvestmentMovementType;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoicePaymentService;
import br.com.kuntzedevprojects.money_master_2.services.finance.installment.InstallmentAnticipationService;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentContributionPlanService;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentProductService;
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
    private final InvestmentProductService investmentProductService = mock(InvestmentProductService.class);
    private final InvestmentContributionPlanService investmentContributionPlanService = mock(InvestmentContributionPlanService.class);
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
            investmentProductService,
            investmentContributionPlanService,
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

    @Test
    void shouldPreviewInvestmentContributionPlanWithoutCreatingPlanItem() {
        InvestmentProduct product = investmentProduct();
        when(investmentProductService.resolveForAi("ana@example.com", "Capitalização", "Banco")).thenReturn(product);

        FinanceCommandBatchResponse response = executor.preview("ana@example.com", new FinanceCommandBatchRequest(
                true,
                "planejar aporte",
                null,
                List.of(investmentPlanCommand())
        ));

        assertThat(response.dryRun()).isTrue();
        assertThat(response.requiresConfirmation()).isTrue();
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo(AiCommandStatus.PREVIEWED);
        verifyNoInteractions(investmentContributionPlanService);
    }

    @Test
    void shouldBlockInvestmentCommandWhenInvestmentSharingIsDisabled() {
        doThrow(new BusinessException("Investimentos desativados"))
                .when(privacySettingsService)
                .requireInvestmentProductsShared("ana@example.com");

        FinanceCommandBatchResponse response = executor.preview("ana@example.com", new FinanceCommandBatchRequest(
                true,
                "planejar aporte",
                null,
                List.of(investmentPlanCommand())
        ));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo(AiCommandStatus.FAILED);
        assertThat(response.results().getFirst().message()).contains("Investimentos desativados");
        verifyNoInteractions(investmentProductService);
        verifyNoInteractions(investmentContributionPlanService);
    }

    @Test
    void shouldRegisterInvestmentContributionAfterConfirmation() {
        User owner = owner();
        InvestmentProduct product = investmentProduct();
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner);
        when(auditRepository.save(any(AiCommandAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(investmentProductService.findOwnedProduct("ana@example.com", product.getId())).thenReturn(product);
        when(investmentProductService.contribute(eq("ana@example.com"), eq(product.getId()), any(InvestmentMovementRequest.class)))
                .thenReturn(new InvestmentMovementResponse(
                        30L,
                        product.getId(),
                        InvestmentMovementType.CONTRIBUTION,
                        new BigDecimal("500.00"),
                        LocalDate.of(2026, 9, 5),
                        "Aporte",
                        TransactionSource.AI_CHAT,
                        "registrado pela IA",
                        null
                ));

        FinanceCommandBatchResponse response = executor.execute("ana@example.com", new FinanceCommandBatchRequest(
                false,
                "registrar aporte",
                "token",
                List.of(investmentContributionCommand())
        ));

        ArgumentCaptor<InvestmentMovementRequest> request = ArgumentCaptor.forClass(InvestmentMovementRequest.class);
        verify(investmentProductService).contribute(eq("ana@example.com"), eq(product.getId()), request.capture());
        assertThat(response.executed()).isTrue();
        assertThat(response.results().getFirst().status()).isEqualTo(AiCommandStatus.EXECUTED);
        assertThat(request.getValue().source()).isEqualTo(TransactionSource.AI_CHAT);
        assertThat(request.getValue().amount()).isEqualByComparingTo("500.00");
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

    private FinanceCommandItem investmentPlanCommand() {
        try {
            return objectMapper.readValue("""
                    {
                      "type": "CREATE_INVESTMENT_CONTRIBUTION_PLAN",
                      "monthlyCycleId": 7,
                      "investmentProductName": "Capitalização",
                      "institutionName": "Banco",
                      "amount": 250.00,
                      "dueDate": "2026-09-10",
                      "recurring": false
                    }
                    """, FinanceCommandItem.class);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private FinanceCommandItem investmentContributionCommand() {
        try {
            return objectMapper.readValue("""
                    {
                      "type": "CONTRIBUTE_INVESTMENT_PRODUCT",
                      "investmentProductId": 10,
                      "amount": 500.00,
                      "description": "Aporte",
                      "occurredOn": "2026-09-05",
                      "notes": "registrado pela IA"
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

    private InvestmentProduct investmentProduct() {
        InvestmentProduct product = new InvestmentProduct();
        product.setId(10L);
        product.setOwner(owner());
        product.setName("Capitalização");
        product.setTypeName("Capitalização");
        product.setInstitutionName("Banco");
        product.setActive(true);
        return product;
    }
}
