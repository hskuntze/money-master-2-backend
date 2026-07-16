package br.com.kuntzedevprojects.money_master_2.services.finance.debt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtCancelRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtResponse;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.Debt;
import br.com.kuntzedevprojects.money_master_2.entities.DebtInstallment;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.AccountType;
import br.com.kuntzedevprojects.money_master_2.enums.DebtAmortizationMethod;
import br.com.kuntzedevprojects.money_master_2.enums.DebtInstallmentStatus;
import br.com.kuntzedevprojects.money_master_2.enums.DebtStatus;
import br.com.kuntzedevprojects.money_master_2.enums.DebtType;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.repositories.DebtRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import br.com.kuntzedevprojects.money_master_2.services.CategoryService;
import br.com.kuntzedevprojects.money_master_2.services.CurrentUserService;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;

class DebtServiceTest {

    private final DebtRepository debtRepository = mock(DebtRepository.class);
    private final MonthlyPlanItemRepository planItemRepository = mock(MonthlyPlanItemRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final FinancialPeriodService financialPeriodService = mock(FinancialPeriodService.class);
    private final DebtService service = new DebtService(
            debtRepository,
            planItemRepository,
            currentUserService,
            accountService,
            categoryService,
            financialPeriodService
    );

    @Test
    void shouldCreateDebtScheduleWithoutCentResidualLoss() {
        User owner = owner();
        Account account = account(owner);
        AtomicLong planId = new AtomicLong(100L);
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner);
        when(accountService.getOrCreateDefaultAccount("ana@example.com")).thenReturn(account);
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> {
            Debt debt = invocation.getArgument(0);
            debt.setId(10L);
            return debt;
        });
        when(financialPeriodService.findOrCreateForDate(anyString(), any(LocalDate.class)))
                .thenAnswer(invocation -> period(owner, invocation.getArgument(1)));
        when(planItemRepository.save(any(MonthlyPlanItem.class))).thenAnswer(invocation -> {
            MonthlyPlanItem item = invocation.getArgument(0);
            item.setId(planId.getAndIncrement());
            return item;
        });

        DebtResponse response = service.create("ana@example.com", new DebtCreateRequest(
                "Emprestimo",
                DebtType.PERSONAL_LOAN,
                DebtAmortizationMethod.CONSTANT_PRINCIPAL,
                null,
                null,
                new BigDecimal("100.00"),
                null,
                null,
                null,
                null,
                3,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 10),
                null
        ));

        assertThat(response.installments())
                .extracting(installment -> installment.principalAmount())
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
        assertThat(response.installments().stream()
                .map(installment -> installment.principalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
        assertThat(response.totalScheduledAmount()).isEqualByComparingTo("100.00");
        assertThat(response.outstandingPrincipalAmount()).isEqualByComparingTo("100.00");

        ArgumentCaptor<MonthlyPlanItem> captor = ArgumentCaptor.forClass(MonthlyPlanItem.class);
        verify(planItemRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(item -> item.getNature() == MonthlyPlanItemNature.DEBT)
                .allMatch(item -> item.getStatus() == MonthlyPlanItemStatus.PENDING);
    }

    @Test
    void shouldCancelOnlyUnpaidInstallmentsAndKeepPaidHistory() {
        User owner = owner();
        Debt debt = debt(owner);
        MonthlyPlanItem paidItem = planItem(owner, new BigDecimal("50.00"), MonthlyPlanItemStatus.PAID);
        MonthlyPlanItem pendingItem = planItem(owner, BigDecimal.ZERO, MonthlyPlanItemStatus.PENDING);
        DebtInstallment paidInstallment = installment(owner, debt, 1, paidItem);
        DebtInstallment pendingInstallment = installment(owner, debt, 2, pendingItem);
        debt.addInstallment(paidInstallment);
        debt.addInstallment(pendingInstallment);
        when(debtRepository.findByIdAndOwnerEmailWithInstallments(10L, "ana@example.com")).thenReturn(Optional.of(debt));

        DebtResponse response = service.cancel("ana@example.com", 10L, new DebtCancelRequest("Renegociada fora do sistema"));

        assertThat(response.status()).isEqualTo(DebtStatus.CANCELED);
        assertThat(paidItem.getStatus()).isEqualTo(MonthlyPlanItemStatus.PAID);
        assertThat(pendingItem.getStatus()).isEqualTo(MonthlyPlanItemStatus.CANCELED);
        assertThat(pendingInstallment.getStatus()).isEqualTo(DebtInstallmentStatus.CANCELED);
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

    private Debt debt(User owner) {
        Debt debt = new Debt();
        debt.setId(10L);
        debt.setOwner(owner);
        debt.setName("Emprestimo");
        debt.setType(DebtType.PERSONAL_LOAN);
        debt.setStatus(DebtStatus.ACTIVE);
        debt.setPrincipalAmount(new BigDecimal("100.00"));
        debt.setInstallmentAmount(new BigDecimal("50.00"));
        debt.setInstallmentCount(2);
        debt.setStartDate(LocalDate.of(2026, 7, 1));
        debt.setFirstDueDate(LocalDate.of(2026, 8, 10));
        debt.setLastDueDate(LocalDate.of(2026, 9, 10));
        return debt;
    }

    private DebtInstallment installment(User owner, Debt debt, int number, MonthlyPlanItem planItem) {
        DebtInstallment installment = new DebtInstallment();
        installment.setId((long) number);
        installment.setOwner(owner);
        installment.setDebt(debt);
        installment.setFinancialPeriod(planItem.getFinancialPeriod());
        installment.setMonthlyPlanItem(planItem);
        installment.setInstallmentNumber(number);
        installment.setDueDate(planItem.getDueDate());
        installment.setPrincipalAmount(new BigDecimal("50.00"));
        installment.setInterestAmount(BigDecimal.ZERO.setScale(2));
        installment.setFeeAmount(BigDecimal.ZERO.setScale(2));
        installment.setTotalAmount(new BigDecimal("50.00"));
        installment.setBalanceAfterPayment(number == 1 ? new BigDecimal("50.00") : BigDecimal.ZERO.setScale(2));
        installment.setStatus(DebtInstallmentStatus.PENDING);
        return installment;
    }

    private MonthlyPlanItem planItem(User owner, BigDecimal actualAmount, MonthlyPlanItemStatus status) {
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setId(status == MonthlyPlanItemStatus.PAID ? 101L : 102L);
        item.setOwner(owner);
        item.setFinancialPeriod(period(owner, status == MonthlyPlanItemStatus.PAID
                ? LocalDate.of(2026, 8, 10)
                : LocalDate.of(2026, 9, 10)));
        item.setDescription("Emprestimo");
        item.setExpectedAmount(new BigDecimal("50.00"));
        item.setActualAmount(actualAmount);
        item.setDueDate(status == MonthlyPlanItemStatus.PAID ? LocalDate.of(2026, 8, 10) : LocalDate.of(2026, 9, 10));
        item.setStatus(status);
        item.setNature(MonthlyPlanItemNature.DEBT);
        if (status == MonthlyPlanItemStatus.PAID) {
            item.setPaidOn(LocalDate.of(2026, 8, 10));
        }
        return item;
    }

    private FinancialPeriod period(User owner, LocalDate date) {
        FinancialPeriod period = new FinancialPeriod();
        period.setId((long) date.getMonthValue());
        period.setOwner(owner);
        period.setName(date.getMonth().name());
        period.setStartDate(date.withDayOfMonth(1));
        period.setEndDate(date.withDayOfMonth(date.lengthOfMonth()));
        period.setStatus(FinancialPeriodStatus.OPEN);
        return period;
    }
}
