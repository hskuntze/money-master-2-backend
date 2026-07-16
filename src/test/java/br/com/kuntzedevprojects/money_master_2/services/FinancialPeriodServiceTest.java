package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.entities.Account;
import br.com.kuntzedevprojects.money_master_2.entities.FinancialPeriod;
import br.com.kuntzedevprojects.money_master_2.entities.MonthlyPlanItem;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialPeriodRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.FinancialTransactionRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.InstallmentPurchaseEntryRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.MonthlyPlanItemRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.PaymentRepository;

class FinancialPeriodServiceTest {

    private static final String OWNER_EMAIL = "ana@example.com";

    private final FinancialPeriodRepository periodRepository = mock(FinancialPeriodRepository.class);
    private final MonthlyPlanItemRepository planItemRepository = mock(MonthlyPlanItemRepository.class);
    private final FinancialTransactionRepository transactionRepository = mock(FinancialTransactionRepository.class);
    private final InstallmentPurchaseEntryRepository installmentEntryRepository = mock(InstallmentPurchaseEntryRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AccountService accountService = mock(AccountService.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final MonthlyPeriodSummaryCalculator summaryCalculator = mock(MonthlyPeriodSummaryCalculator.class);
    private final FinancialPeriodService service = new FinancialPeriodService(
            periodRepository,
            planItemRepository,
            transactionRepository,
            installmentEntryRepository,
            paymentRepository,
            currentUserService,
            accountService,
            categoryService,
            summaryCalculator
    );

    @Test
    void shouldRejectDirectActualAmountUpdate() {
        MonthlyPlanItem item = planItem(MonthlyPlanItemStatus.PENDING, money("0.00"));
        when(planItemRepository.findByIdAndOwnerEmail(10L, OWNER_EMAIL)).thenReturn(Optional.of(item));

        MonthlyPlanItemUpdateRequest request = updateRequest(
                null,
                money("50.00"),
                null,
                null
        );

        assertThatThrownBy(() -> service.updatePlanItem(OWNER_EMAIL, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("valor realizado");
    }

    @Test
    void shouldRejectDirectStatusChangeToPaid() {
        MonthlyPlanItem item = planItem(MonthlyPlanItemStatus.PENDING, money("0.00"));
        when(planItemRepository.findByIdAndOwnerEmail(10L, OWNER_EMAIL)).thenReturn(Optional.of(item));

        MonthlyPlanItemUpdateRequest request = updateRequest(
                null,
                null,
                null,
                MonthlyPlanItemStatus.PAID
        );

        assertThatThrownBy(() -> service.updatePlanItem(OWNER_EMAIL, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("baixa");
    }

    @Test
    void shouldRejectCreatingPlanItemAlreadyPaid() {
        User owner = owner();
        FinancialPeriod period = period(owner);
        when(currentUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(periodRepository.findByIdAndOwnerEmail(1L, OWNER_EMAIL)).thenReturn(Optional.of(period));
        when(accountService.getOrCreateDefaultAccount(OWNER_EMAIL)).thenReturn(account(owner));

        MonthlyPlanItemCreateRequest request = new MonthlyPlanItemCreateRequest(
                null,
                null,
                TransactionType.EXPENSE,
                "Aluguel",
                money("1200.00"),
                LocalDate.of(2026, 7, 10),
                null,
                null,
                null,
                false,
                null,
                MonthlyPlanItemStatus.PAID,
                null,
                null
        );

        assertThatThrownBy(() -> service.createPlanItem(OWNER_EMAIL, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("baixa");
    }

    private MonthlyPlanItemUpdateRequest updateRequest(
            BigDecimal expectedAmount,
            BigDecimal actualAmount,
            LocalDate paidOn,
            MonthlyPlanItemStatus status
    ) {
        return new MonthlyPlanItemUpdateRequest(
                null,
                null,
                null,
                null,
                expectedAmount,
                actualAmount,
                null,
                paidOn,
                null,
                null,
                null,
                null,
                null,
                status,
                null,
                null
        );
    }

    private MonthlyPlanItem planItem(MonthlyPlanItemStatus status, BigDecimal actualAmount) {
        User owner = owner();
        MonthlyPlanItem item = new MonthlyPlanItem();
        item.setId(10L);
        item.setOwner(owner);
        item.setFinancialPeriod(period(owner));
        item.setType(TransactionType.EXPENSE);
        item.setDescription("Aluguel");
        item.setExpectedAmount(money("1200.00"));
        item.setActualAmount(actualAmount);
        item.setDueDate(LocalDate.of(2026, 7, 10));
        item.setStatus(status);
        item.setAggregationType(MonthlyPlanItemAggregationType.NORMAL);
        return item;
    }

    private FinancialPeriod period(User owner) {
        FinancialPeriod period = new FinancialPeriod();
        period.setId(1L);
        period.setOwner(owner);
        period.setName("Julho");
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.setStatus(FinancialPeriodStatus.OPEN);
        return period;
    }

    private Account account(User owner) {
        Account account = new Account();
        account.setId(1L);
        account.setOwner(owner);
        account.setName("Conta principal");
        return account;
    }

    private User owner() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana");
        user.setEmail(OWNER_EMAIL);
        return user;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
