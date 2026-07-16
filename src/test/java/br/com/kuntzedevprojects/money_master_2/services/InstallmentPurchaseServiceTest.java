package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.InstallmentPurchase;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentEntryStatus;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentSource;
import br.com.kuntzedevprojects.money_master_2.enums.InstallmentPaymentMode;
import br.com.kuntzedevprojects.money_master_2.repositories.CreditCardInvoiceItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseEntryRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardInvoiceService;
import br.com.kuntzedevprojects.money_master_2.services.finance.creditcard.CreditCardService;

class InstallmentPurchaseServiceTest {

    private final InstallmentPurchaseRepository purchaseRepository = mock(InstallmentPurchaseRepository.class);
    private final InstallmentPurchaseEntryRepository entryRepository = mock(InstallmentPurchaseEntryRepository.class);
    private final MonthlyPlanItemRepository planItemRepository = mock(MonthlyPlanItemRepository.class);
    private final CreditCardInvoiceItemRepository invoiceItemRepository = mock(CreditCardInvoiceItemRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final CreditCardService creditCardService = mock(CreditCardService.class);
    private final CreditCardInvoiceService creditCardInvoiceService = mock(CreditCardInvoiceService.class);
    private final InstallmentPurchaseService service = new InstallmentPurchaseService(
            purchaseRepository,
            entryRepository,
            planItemRepository,
            invoiceItemRepository,
            currentUserService,
            accountService,
            categoryService,
            financialPeriodService,
            creditCardService,
            creditCardInvoiceService
    );

    @Test
    void shouldDistributeCentResidualToLastInstallmentWhenTotalAmountIsAuthoritative() {
        User owner = owner();
        Account account = account(owner);
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner);
        when(accountService.getOrCreateDefaultAccount("ana@example.com")).thenReturn(account);
        when(purchaseRepository.save(any(InstallmentPurchase.class))).thenAnswer(invocation -> {
            InstallmentPurchase purchase = invocation.getArgument(0);
            purchase.setId(10L);
            return purchase;
        });
        when(financialPeriodService.findOrCreateForDate(anyString(), any(LocalDate.class)))
                .thenAnswer(invocation -> period(owner, invocation.getArgument(1)));
        when(planItemRepository.save(any(MonthlyPlanItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstallmentPurchaseResponse response = service.create("ana@example.com", new InstallmentPurchaseCreateRequest(
                "Curso",
                new BigDecimal("100.00"),
                3,
                null,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 10),
                null,
                InstallmentPaymentMode.DIRECT_PAYABLE,
                null,
                null
        ));

        assertThat(response.entries())
                .extracting(entry -> entry.amount())
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
        assertThat(response.entries().stream()
                .map(entry -> entry.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldNotAutomaticallyMarkDirectInstallmentAsPaidWhenCycleIsClosed() {
        User owner = owner();
        Account account = account(owner);
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner);
        when(accountService.getOrCreateDefaultAccount("ana@example.com")).thenReturn(account);
        when(purchaseRepository.save(any(InstallmentPurchase.class))).thenAnswer(invocation -> {
            InstallmentPurchase purchase = invocation.getArgument(0);
            purchase.setId(11L);
            return purchase;
        });
        when(financialPeriodService.findOrCreateForDate(anyString(), any(LocalDate.class)))
                .thenAnswer(invocation -> period(owner, invocation.getArgument(1), FinancialPeriodStatus.CLOSED));
        when(planItemRepository.save(any(MonthlyPlanItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstallmentPurchaseResponse response = service.create("ana@example.com", new InstallmentPurchaseCreateRequest(
                "Sofa",
                new BigDecimal("250.00"),
                1,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 10),
                null,
                InstallmentPaymentMode.DIRECT_PAYABLE,
                null,
                null
        ));

        assertThat(response.paidInstallments()).isZero();
        assertThat(response.pendingInstallments()).isEqualTo(1);
        assertThat(response.entries().getFirst().status()).isEqualTo(InstallmentEntryStatus.POSTED);
        assertThat(response.entries().getFirst().paymentSource()).isEqualTo(InstallmentPaymentSource.NONE);
    }

    private User owner() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("Ana");
        owner.setEmail("ana@example.com");
        return owner;
    }

    private Account account(User owner) {
        Account account = new Account();
        account.setId(2L);
        account.setOwner(owner);
        account.setName("Conta");
        account.setType(AccountType.CHECKING);
        return account;
    }

    private FinancialPeriod period(User owner, LocalDate date) {
        return period(owner, date, FinancialPeriodStatus.OPEN);
    }

    private FinancialPeriod period(User owner, LocalDate date, FinancialPeriodStatus status) {
        FinancialPeriod period = new FinancialPeriod();
        period.setId((long) date.getMonthValue());
        period.setOwner(owner);
        period.setName(date.getMonth().name());
        period.setStartDate(date.withDayOfMonth(1));
        period.setEndDate(date.withDayOfMonth(date.lengthOfMonth()));
        period.setStatus(status);
        return period;
    }
}
