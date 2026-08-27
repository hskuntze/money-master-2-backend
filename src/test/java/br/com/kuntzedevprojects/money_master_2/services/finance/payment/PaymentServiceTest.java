package br.com.kuntzedevprojects.money_master_2.services.finance.payment;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentReverseRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentMovementRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialTransaction;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.Payment;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentMethod;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentContributionPlanService;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentProductService;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final FinancialTransactionService transactionService = mock(FinancialTransactionService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final SavingsJarService savingsJarService = mock(SavingsJarService.class);
    private final InvestmentProductService investmentProductService = mock(InvestmentProductService.class);
    private final PaymentService service = new PaymentService(
            paymentRepository,
            transactionRepository,
            financialPeriodService,
            transactionService,
            currentUserService,
            accountService,
            categoryService,
            savingsJarService,
            investmentProductService
    );

    @Test
    void shouldRegisterPartialPayablePaymentWithoutTransactionAndSyncLegacyItem() {
        MonthlyPlanItem payable = payable();
        Account account = account();
        when(financialPeriodService.findOwnedPlanItem("ana@example.com", 10L)).thenReturn(payable);
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(payable.getOwner());
        when(accountService.findOwnedAccount("ana@example.com", 20L)).thenReturn(account);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(30L);
            return payment;
        });
        when(paymentRepository.sumActiveByPayable(10L)).thenReturn(new BigDecimal("400.00"));
        when(paymentRepository.findLatestActivePaymentDateByPlanItem(10L)).thenReturn(LocalDate.of(2026, 6, 10));

        service.registerPayablePayment("ana@example.com", 10L, new PaymentRequest(
                null,
                20L,
                null,
                new BigDecimal("400.00"),
                LocalDate.of(2026, 6, 10),
                PaymentMethod.PIX,
                null,
                false,
                "Pagamento parcial"
        ));

        assertAll(
                () -> assertEquals(new BigDecimal("400.00"), payable.getActualAmount()),
                () -> assertEquals(LocalDate.of(2026, 6, 10), payable.getPaidOn()),
                () -> assertEquals(MonthlyPlanItemStatus.PARTIALLY_PAID, payable.getStatus())
        );
    }

    @Test
    void shouldRegisterFullPayablePaymentCreatingCashTransactionAndMarkLegacyItemAsPaid() {
        MonthlyPlanItem payable = payable();
        Account account = account();
        FinancialTransaction transaction = transaction(payable, account);
        when(financialPeriodService.findOwnedPlanItem("ana@example.com", 10L)).thenReturn(payable);
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(payable.getOwner());
        when(accountService.findOwnedAccount("ana@example.com", 20L)).thenReturn(account);
        when(transactionService.create(any(String.class), any(FinancialTransactionCreateRequest.class)))
                .thenReturn(FinancialTransactionResponse.from(transaction));
        when(transactionService.findOwnedTransaction("ana@example.com", 40L)).thenReturn(transaction);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(30L);
            payment.setTransaction(transaction);
            return payment;
        });
        when(paymentRepository.sumActiveByPayable(10L)).thenReturn(new BigDecimal("1000.00"));
        when(paymentRepository.findLatestActivePaymentDateByPlanItem(10L)).thenReturn(LocalDate.of(2026, 6, 10));

        service.registerPayablePayment("ana@example.com", 10L, new PaymentRequest(
                null,
                20L,
                null,
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 6, 10),
                PaymentMethod.PIX,
                null,
                true,
                "Pagamento completo"
        ));

        ArgumentCaptor<FinancialTransactionCreateRequest> transactionRequest = ArgumentCaptor.forClass(FinancialTransactionCreateRequest.class);
        verify(transactionService).create(any(String.class), transactionRequest.capture());
        assertAll(
                () -> assertEquals(new BigDecimal("1000.00"), payable.getActualAmount()),
                () -> assertEquals(LocalDate.of(2026, 6, 10), payable.getPaidOn()),
                () -> assertEquals(MonthlyPlanItemStatus.PAID, payable.getStatus()),
                () -> assertEquals(payable.getId(), transactionRequest.getValue().monthlyPlanItemId()),
                () -> assertEquals(payable.getFinancialPeriod().getId(), transactionRequest.getValue().financialPeriodId()),
                () -> assertEquals(TransactionType.EXPENSE, transactionRequest.getValue().type())
        );
    }

    @Test
    void shouldRegisterInvestmentContributionWhenInvestmentPayableIsPaid() {
        MonthlyPlanItem payable = payable();
        payable.setNature(MonthlyPlanItemNature.INVESTMENT);
        payable.setNotes(InvestmentContributionPlanService.NOTES_PREFIX + "77");
        Account account = account();
        when(financialPeriodService.findOwnedPlanItem("ana@example.com", 10L)).thenReturn(payable);
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(payable.getOwner());
        when(accountService.findOwnedAccount("ana@example.com", 20L)).thenReturn(account);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(30L);
            return payment;
        });
        when(paymentRepository.sumActiveByPayable(10L)).thenReturn(new BigDecimal("1000.00"));
        when(paymentRepository.findLatestActivePaymentDateByPlanItem(10L)).thenReturn(LocalDate.of(2026, 6, 10));

        service.registerPayablePayment("ana@example.com", 10L, new PaymentRequest(
                null,
                20L,
                null,
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 6, 10),
                PaymentMethod.PIX,
                null,
                false,
                "Aporte realizado"
        ));

        ArgumentCaptor<InvestmentMovementRequest> movementRequest = ArgumentCaptor.forClass(InvestmentMovementRequest.class);
        verify(investmentProductService).contribute(any(String.class), any(Long.class), movementRequest.capture());
        assertAll(
                () -> assertEquals(new BigDecimal("1000.00"), movementRequest.getValue().amount()),
                () -> assertEquals(LocalDate.of(2026, 6, 10), movementRequest.getValue().occurredOn())
        );
    }

    @Test
    void shouldReversePaymentWithoutDeletingLinkedTransactionAndDelegateLegacyStateRecalculation() {
        MonthlyPlanItem payable = payable();
        payable.setActualAmount(new BigDecimal("1000.00"));
        payable.setStatus(MonthlyPlanItemStatus.PAID);
        FinancialTransaction transaction = transaction(payable, account());
        Payment payment = new Payment();
        payment.setId(30L);
        payment.setOwner(payable.getOwner());
        payment.setCycle(payable.getFinancialPeriod());
        payment.setPayable(payable);
        payment.setTransaction(transaction);
        payment.setAccount(account());
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setPaymentDate(LocalDate.of(2026, 6, 10));
        payment.setMethod(PaymentMethod.PIX);
        payment.setStatus(PaymentStatus.ACTIVE);
        when(paymentRepository.findByIdAndOwnerEmail(30L, "ana@example.com")).thenReturn(java.util.Optional.of(payment));
        when(paymentRepository.sumActiveByPayable(10L)).thenReturn(BigDecimal.ZERO);

        service.reverse("ana@example.com", 30L, new PaymentReverseRequest(
                false,
                "Lancamento duplicado"
        ));

        assertAll(
                () -> assertEquals(PaymentStatus.REVERSED, payment.getStatus()),
                () -> assertNull(transaction.getMonthlyPlanItem()),
                () -> verify(financialPeriodService).synchronizePlanItemPayment(payable)
        );
    }

    private MonthlyPlanItem payable() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("ana@example.com");

        FinancialPeriod cycle = new FinancialPeriod();
        cycle.setId(2L);
        cycle.setOwner(owner);
        cycle.setName("Junho");
        cycle.setStartDate(LocalDate.of(2026, 6, 1));
        cycle.setEndDate(LocalDate.of(2026, 6, 30));
        cycle.setStatus(FinancialPeriodStatus.OPEN);

        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setId(10L);
        item.setOwner(owner);
        item.setFinancialPeriod(cycle);
        item.setAccount(account());
        item.setType(TransactionType.EXPENSE);
        item.setDescription("Aluguel");
        item.setExpectedAmount(new BigDecimal("1000.00"));
        item.setActualAmount(BigDecimal.ZERO);
        item.setDueDate(LocalDate.of(2026, 6, 5));
        item.setStatus(MonthlyPlanItemStatus.PENDING);
        item.setNature(MonthlyPlanItemNature.FIXED);
        item.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
        return item;
    }

    private Account account() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("ana@example.com");

        Account account = new Account();
        account.setId(20L);
        account.setOwner(owner);
        account.setName("Conta");
        account.setType(AccountType.CHECKING);
        return account;
    }

    private FinancialTransaction transaction(MonthlyPlanItem payable, Account account) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setId(40L);
        transaction.setOwner(payable.getOwner());
        transaction.setAccount(account);
        transaction.setFinancialPeriod(payable.getFinancialPeriod());
        transaction.setMonthlyPlanItem(payable);
        transaction.setType(payable.getType());
        transaction.setDescription(payable.getDescription());
        transaction.setAmount(payable.getExpectedAmount());
        transaction.setOccurredOn(LocalDate.of(2026, 6, 10));
        return transaction;
    }
}
