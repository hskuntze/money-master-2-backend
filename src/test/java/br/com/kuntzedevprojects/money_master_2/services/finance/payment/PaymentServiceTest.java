package br.com.kuntzedevprojects.money_master_2.services.finance.payment;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.payment.PaymentRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.Payment;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.PaymentMethod;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final FinancialTransactionService transactionService = mock(FinancialTransactionService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final SavingsJarService savingsJarService = mock(SavingsJarService.class);
    private final PaymentService service = new PaymentService(
            paymentRepository,
            transactionRepository,
            financialPeriodService,
            transactionService,
            currentUserService,
            accountService,
            categoryService,
            savingsJarService
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
}
